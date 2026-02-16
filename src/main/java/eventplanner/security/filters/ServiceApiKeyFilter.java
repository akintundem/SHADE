package eventplanner.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import eventplanner.security.config.ServiceAuthProperties;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * Validates service-to-service traffic using an `X-API-Key` header injected by Kong.
 * 
 * Security behavior:
 * - If service auth is disabled: pass through to JWT auth
 * - If require-header is true: X-API-Key MUST be present and valid (Kong gateway enforcement)
 * - If API key is present but invalid: reject with 403
 * - If API key is valid: optionally set service authentication for allowed paths
 * - Requests with valid Bearer token bypass API key requirement (user requests via mobile app)
 */
@Component
public class ServiceApiKeyFilter extends OncePerRequestFilter {

    private final String expectedApiKey;
    private final boolean serviceAuthEnabled;
    private final boolean requireApiKeyHeader;
    private final String allowServiceRolePaths;

    private final ObjectMapper objectMapper;

    private static final String HEADER_NAME = "X-API-Key";

    public ServiceApiKeyFilter(ObjectMapper objectMapper, ServiceAuthProperties serviceAuthProperties) {
        this.objectMapper = objectMapper;
        this.expectedApiKey = serviceAuthProperties.getApiKey();
        this.serviceAuthEnabled = requireConfigured(serviceAuthProperties.getEnabled(), "service.auth.enabled");
        this.requireApiKeyHeader = requireConfigured(serviceAuthProperties.getRequireHeader(), "service.auth.require-header");
        this.allowServiceRolePaths = serviceAuthProperties.getAllowServiceRolePaths();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Allow actuator endpoints without service auth for container health checks
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // If service auth is entirely disabled, skip all validation
        if (!serviceAuthEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Internal/service paths must always validate API key (no Bearer bypass)
        // to enforce gateway-only access for trusted service routes.
        boolean isInternalPath = isServicePathAllowed(path);
        if (!isInternalPath) {
            String authorizationHeader = request.getHeader("Authorization");
            boolean hasBearerToken = StringUtils.hasText(authorizationHeader) &&
                                    authorizationHeader.trim().startsWith("Bearer ");
            if (hasBearerToken) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // No Bearer token - validate API key for service-to-service requests
        String apiKey = request.getHeader(HEADER_NAME);

        // Case 1: No API key provided
        if (!StringUtils.hasText(apiKey)) {
            // If require-header is true, the API key is mandatory for service requests
            // This enforces that all service traffic comes through Kong (which injects X-API-Key)
            if (requireApiKeyHeader) {
                sendForbiddenResponse(response, "Missing required X-API-Key header", path);
                return;
            }
            // Dev mode: pass through if header enforcement is disabled
            filterChain.doFilter(request, response);
            return;
        }

        // Case 2: API key is present - validate it
        if (!StringUtils.hasText(expectedApiKey)) {
            // Expected API key not configured - this is a configuration error in production
            if (requireApiKeyHeader) {
                sendForbiddenResponse(response, "Service API key not configured on server", path);
                return;
            }
            // Dev mode: pass through if header enforcement is disabled
            filterChain.doFilter(request, response);
            return;
        }

        if (!constantTimeEquals(apiKey, expectedApiKey)) {
            // Invalid API key - always reject
            sendForbiddenResponse(response, "Invalid service API key", request.getRequestURI());
            return;
        }

        // Case 3: Valid API key - attach service-level authentication for allowed paths
        if (SecurityContextHolder.getContext().getAuthentication() == null && isServicePathAllowed(path)) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "service",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // API key is valid, continue
        filterChain.doFilter(request, response);
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     * Uses MessageDigest.isEqual which is designed for cryptographic comparisons.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }

    /**
     * Check if the given path is allowed for service-level authentication.
     * Only specific internal API paths should get ROLE_SERVICE.
     */
    private boolean isServicePathAllowed(String path) {
        if (!StringUtils.hasText(allowServiceRolePaths)) {
            return false;
        }
        for (String prefix : allowServiceRolePaths.split(",")) {
            String trimmed = prefix.trim();
            if (!trimmed.isEmpty() && path.startsWith(trimmed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Send a 403 Forbidden response with JSON body.
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message, String path) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "status", HttpServletResponse.SC_FORBIDDEN,
                "error", "Forbidden",
                "message", message,
                "path", path
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static boolean requireConfigured(Boolean value, String propertyName) {
        return eventplanner.common.util.Preconditions.requireConfigured(value, propertyName);
    }
}
