package ai.eventplanner.auth.security;

import ai.eventplanner.auth.service.RateLimitingService;
import ai.eventplanner.auth.service.UserPrincipal;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Filter to implement rate limiting per user (authenticated) or IP (unauthenticated)
 * This provides proper protection against abuse by individual users
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class RateLimitingFilter implements Filter {

    private final RateLimitingService rateLimitingService;

    // Endpoints that don't require rate limiting
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/actuator/health",
            "/actuator/info",
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

        // Determine rate limit key based on authentication status
        String rateLimitKey = determineRateLimitKey(httpRequest);
        
        // Check rate limit
        if (!rateLimitingService.isWithinRateLimit(rateLimitKey, requestPath)) {
            sendRateLimitExceededResponse(httpResponse, rateLimitKey, requestPath);
            return;
        }

        // Add rate limit headers
        RateLimitingService.RateLimitInfo rateLimitInfo = rateLimitingService.getRateLimitInfo(rateLimitKey, requestPath);
        httpResponse.setHeader("X-RateLimit-Limit-Minute", String.valueOf(rateLimitInfo.limitMinute));
        httpResponse.setHeader("X-RateLimit-Remaining-Minute", String.valueOf(rateLimitInfo.limitMinute - rateLimitInfo.currentMinute));
        httpResponse.setHeader("X-RateLimit-Limit-Hour", String.valueOf(rateLimitInfo.limitHour));
        httpResponse.setHeader("X-RateLimit-Remaining-Hour", String.valueOf(rateLimitInfo.limitHour - rateLimitInfo.currentHour));

        chain.doFilter(request, response);
    }

    /**
     * Determine the rate limit key based on authentication status
     * - For authenticated users: use user ID from SecurityContext (SECURE)
     * - For unauthenticated requests: use IP address
     */
    private String determineRateLimitKey(HttpServletRequest request) {
        // Extract user ID from Spring Security context (SECURE - cannot be manipulated)
        UUID userId = getCurrentUserId();
        if (userId != null) {
            return "user:" + userId.toString();
        }
        
        // Fallback to IP-based rate limiting for unauthenticated requests
        String clientIp = getClientIp(request);
        return "ip:" + clientIp;
    }
    
    /**
     * Extract user ID from Spring Security context (SECURE - cannot be manipulated)
     * This is the most secure approach as it relies on the authenticated user context
     */
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
    
    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        
        return request.getRemoteAddr();
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private void sendRateLimitExceededResponse(HttpServletResponse response, String rateLimitKey, String endpoint) 
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60"); // Retry after 1 minute
        
        String keyType = rateLimitKey.startsWith("user:") ? "user" : "IP";
        String keyValue = rateLimitKey.substring(rateLimitKey.indexOf(":") + 1);
        
        response.getWriter().write(String.format(
            "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests from %s %s for endpoint %s\",\"status\":429,\"timestamp\":\"%s\",\"retryAfter\":60}",
            keyType, keyValue, endpoint, java.time.Instant.now()
        ));
    }
}
