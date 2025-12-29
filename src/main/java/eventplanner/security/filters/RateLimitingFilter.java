package eventplanner.security.filters;

import eventplanner.security.auth.service.RateLimitingService;
import eventplanner.security.auth.service.UserPrincipal;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filter to implement rate limiting per user (authenticated) or IP (unauthenticated)
 * This provides proper protection against abuse by individual users
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter implements Filter {

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("^\\d+$");

    private final RateLimitingService rateLimitingService;

    // Endpoints that don't require rate limiting
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/actuator/health",
            "/actuator/info",
            "/api/v1/auth/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/error",
            "/favicon.ico"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestPath = httpRequest.getRequestURI();
        
        // Skip rate limiting for excluded paths
        if (isExcludedPath(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        String endpointKey = determineEndpointKey(httpRequest);

        // Determine rate limit key based on authentication status
        String rateLimitKey = determineRateLimitKey(httpRequest);
        
        // Check rate limit
        if (!rateLimitingService.isWithinRateLimit(rateLimitKey, endpointKey)) {
            sendRateLimitExceededResponse(httpResponse, rateLimitKey, endpointKey);
            return;
        }

        // Add rate limit headers
        RateLimitingService.RateLimitInfo rateLimitInfo = rateLimitingService.getRateLimitInfo(rateLimitKey, endpointKey);
        httpResponse.setHeader("X-RateLimit-Limit-Minute", String.valueOf(rateLimitInfo.limitMinute));
        httpResponse.setHeader("X-RateLimit-Remaining-Minute", String.valueOf(rateLimitInfo.limitMinute - rateLimitInfo.currentMinute));
        httpResponse.setHeader("X-RateLimit-Limit-Hour", String.valueOf(rateLimitInfo.limitHour));
        httpResponse.setHeader("X-RateLimit-Remaining-Hour", String.valueOf(rateLimitInfo.limitHour - rateLimitInfo.currentHour));

        chain.doFilter(request, response);
    }

    /**
     * Determine the rate limit key based on authentication status.
     * - Authenticated requests: user ID from the server-side SecurityContext
     * - Unauthenticated requests: client IP address
     */
    private String determineRateLimitKey(HttpServletRequest request) {
        // Extract user ID from Spring Security context rather than trusting request input
        UUID userId = getCurrentUserId();
        if (userId != null) {
            return "user:" + userId.toString();
        }
        
        // Fallback to IP-based rate limiting for unauthenticated requests
        String clientIp = getClientIp(request);
        return "ip:" + clientIp;
    }
    
    /**
     * Resolve canonical endpoint key by normalizing dynamic segments (UUIDs/ids) to "{id}".
     */
    private String determineEndpointKey(HttpServletRequest request) {
        String canonicalPath = canonicalizePath(request.getRequestURI());
        return request.getMethod().toUpperCase(Locale.US) + ":" + canonicalPath;
    }
    
    /**
     * Collapse path parameters into a stable token so rate limits group similar routes.
     */
    private String canonicalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        StringBuilder builder = new StringBuilder();
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            builder.append('/');
            if (UUID_PATTERN.matcher(segment).matches() || DIGIT_PATTERN.matcher(segment).matches()) {
                builder.append("{id}");
            } else {
                builder.append(segment);
            }
        }
        return builder.length() == 0 ? "/" : builder.toString();
    }
    
    // Extract user ID from server-maintained authentication context (not user-supplied data)
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) principal;
            return userPrincipal.getUser().getId();
        }
        
        return null;
    }
    
    // Exclude health/docs/error endpoints from rate limiting to avoid polluting metrics
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    // Extract client IP from standard headers, falling back to remote address
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Sends rate limit exceeded response without exposing sensitive identifiers.
     */
    private void sendRateLimitExceededResponse(HttpServletResponse response, String rateLimitKey, String endpoint) 
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60"); // Retry after 1 minute
        
        // Do not expose user ID, IP address, or endpoint details to prevent information leakage
        response.getWriter().write(String.format(
            "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\",\"status\":429,\"timestamp\":\"%s\",\"retryAfter\":60}",
            java.time.Instant.now()
        ));
    }
}
