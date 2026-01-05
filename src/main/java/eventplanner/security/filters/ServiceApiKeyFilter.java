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
 * Allows trusted service-to-service traffic using an `X-API-Key` header while letting user traffic pass through.
 * Invalid keys short-circuit with a 403 JSON response.
 */
@Component
public class ServiceApiKeyFilter extends OncePerRequestFilter {

    @Value("${service.auth.api-key:}")
    private String expectedApiKey;

    @Value("${service.auth.enabled:true}")
    private boolean serviceAuthEnabled;

    @Value("${service.auth.require-header:false}")
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
        
        // Allow actuator endpoints without service auth to keep container health checks working
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // If service auth is disabled, skip validation
        if (!serviceAuthEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(HEADER_NAME);

        // Require Kong-injected header when enabled to prevent direct access
        if (requireApiKeyHeader && !StringUtils.hasText(apiKey)) {
            sendForbiddenResponse(response, "Missing service API key", request.getRequestURI());
            return;
        }

        // If API key is present, it must be valid
        if (!StringUtils.hasText(expectedApiKey) || !constantTimeEquals(apiKey, expectedApiKey)) {
            sendForbiddenResponse(response, "Invalid service API key", request.getRequestURI());
            return;
        }

        // Attach a service-level authentication token so downstream auth checks pass.
        if (SecurityContextHolder.getContext().getAuthentication() == null && isServicePathAllowed(request.getRequestURI())) {
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
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }

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
