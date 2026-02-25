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
    /** Secondary key for zero-downtime rotation. Accepted alongside the primary. */
    private final String secondaryApiKey;
    private final boolean serviceAuthEnabled;
    private final boolean requireApiKeyHeader;
    private final String allowServiceRolePaths;

    private final ObjectMapper objectMapper;

    private static final String HEADER_NAME = "X-API-Key";

    public ServiceApiKeyFilter(ObjectMapper objectMapper, ServiceAuthProperties serviceAuthProperties) {
        this.objectMapper = objectMapper;
        this.expectedApiKey = serviceAuthProperties.getApiKey();
        this.secondaryApiKey = serviceAuthProperties.getApiKeySecondary();
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

        boolean isInternalPath = isServicePathAllowed(path);
        String apiKey = request.getHeader(HEADER_NAME);

        // If an X-API-Key header is present, validate it.
        // Exception: if the request also carries a Bearer token it is a user request routed
        // through the gateway — let JWT authentication handle it and skip API key enforcement.
        if (StringUtils.hasText(apiKey)) {
            String authorizationHeader = request.getHeader("Authorization");
            boolean hasBearerToken = StringUtils.hasText(authorizationHeader) &&
                                    authorizationHeader.trim().startsWith("Bearer ");

            if (hasBearerToken) {
                // User request via gateway — JWT auth will validate the Bearer token.
                filterChain.doFilter(request, response);
                return;
            }

            if (!StringUtils.hasText(expectedApiKey)) {
                if (requireApiKeyHeader) {
                    sendForbiddenResponse(response, "Service API key not configured on server", path);
                    return;
                }
                filterChain.doFilter(request, response);
                return;
            }
            // Accept primary key or secondary key (for zero-downtime rotation).
            boolean primaryMatch = constantTimeEquals(apiKey, expectedApiKey);
            boolean secondaryMatch = StringUtils.hasText(secondaryApiKey) && constantTimeEquals(apiKey, secondaryApiKey);
            if (!primaryMatch && !secondaryMatch) {
                sendForbiddenResponse(response, "Invalid service API key", path);
                return;
            }
            // Valid API key — attach service role for internal paths
            if (SecurityContextHolder.getContext().getAuthentication() == null && isInternalPath) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "service",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // No API key present.
        // Non-internal paths with a valid Bearer token are user requests — no key required.
        if (!isInternalPath) {
            String authorizationHeader = request.getHeader("Authorization");
            boolean hasBearerToken = StringUtils.hasText(authorizationHeader) &&
                                    authorizationHeader.trim().startsWith("Bearer ");
            if (hasBearerToken) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // No API key and no Bearer token (or internal path without a key).
        if (requireApiKeyHeader) {
            sendForbiddenResponse(response, "Missing required X-API-Key header", path);
            return;
        }
        // Dev mode: pass through
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
