package ai.eventplanner.auth.security;

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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter to implement rate limiting per client
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
        String method = httpRequest.getMethod();
        
        // Skip rate limiting for excluded paths
        if (isExcludedPath(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        // Extract client ID
        String clientId = httpRequest.getHeader("X-Client-ID");
        if (clientId == null || clientId.trim().isEmpty()) {
            // Apply default rate limiting even without client ID
            String defaultClientId = "anonymous";
            if (!rateLimitingService.isWithinRateLimit(defaultClientId, requestPath)) {
                sendRateLimitExceededResponse(httpResponse, defaultClientId, requestPath);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // Check rate limit
        if (!rateLimitingService.isWithinRateLimit(clientId, requestPath)) {
            sendRateLimitExceededResponse(httpResponse, clientId, requestPath);
            return;
        }

        // Add rate limit headers
        RateLimitingService.RateLimitInfo rateLimitInfo = rateLimitingService.getRateLimitInfo(clientId, requestPath);
        httpResponse.setHeader("X-RateLimit-Limit-Minute", String.valueOf(rateLimitInfo.limitMinute));
        httpResponse.setHeader("X-RateLimit-Remaining-Minute", String.valueOf(rateLimitInfo.limitMinute - rateLimitInfo.currentMinute));
        httpResponse.setHeader("X-RateLimit-Limit-Hour", String.valueOf(rateLimitInfo.limitHour));
        httpResponse.setHeader("X-RateLimit-Remaining-Hour", String.valueOf(rateLimitInfo.limitHour - rateLimitInfo.currentHour));

        chain.doFilter(request, response);
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private void sendRateLimitExceededResponse(HttpServletResponse response, String clientId, String endpoint) 
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60"); // Retry after 1 minute
        
        response.getWriter().write(String.format(
            "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests from client %s for endpoint %s\",\"status\":429,\"timestamp\":\"%s\",\"retryAfter\":60}",
            clientId, endpoint, java.time.Instant.now()
        ));
    }
}
