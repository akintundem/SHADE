package eventplanner.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
 * - If API key header is required but missing: reject with 403
 * - If API key is present but invalid: reject with 403
 * - If API key is valid: optionally set service authentication for allowed paths
 * - If no API key and not required: pass through to JWT auth
 */
@Component
public class ServiceApiKeyFilter extends OncePerRequestFilter {

    @Value("${service.auth.api-key:}")
    private String expectedApiKey;

    @Value("${service.auth.enabled:true}")
    private boolean serviceAuthEnabled;

    @Value("${service.auth.require-header:true}")
    private boolean requireApiKeyHeader;

    @Value("${service.auth.allow-service-role-paths:}")
    private String allowServiceRolePaths;

    private final ObjectMapper objectMapper;

    private static final String HEADER_NAME = "X-API-Key";

    public ServiceApiKeyFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

        String apiKey = request.getHeader(HEADER_NAME);

        // Case 1: No API key provided
        if (!StringUtils.hasText(apiKey)) {
            if (requireApiKeyHeader) {
                // Header is required but missing - reject
                sendForbiddenResponse(response, "Missing service API key", request.getRequestURI());
                return;
            }
            // Header not required and not present - continue to JWT auth
            filterChain.doFilter(request, response);
            return;
        }

        // Case 2: API key is present - it MUST be valid
        if (!StringUtils.hasText(expectedApiKey)) {
            // No expected key configured but one was provided - reject to prevent misconfiguration
            sendForbiddenResponse(response, "Service authentication not configured", request.getRequestURI());
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
}
