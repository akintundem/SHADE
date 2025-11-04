package eventplanner.security.filters;

import eventplanner.security.auth.service.ClientValidationService;
import eventplanner.common.exception.UnauthorizedException;
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
 * Filter to validate client ID on all requests
 * This provides an extra layer of security and debugging capability
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class ClientValidationFilter implements Filter {

    private final ClientValidationService clientValidationService;

    // Endpoints that don't require client ID validation
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/actuator/health",
            "/actuator/info",
            "/api/v1/auth/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/error",
            "/favicon.ico",
            "/images/",
            "/css/",
            "/js/"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestPath = httpRequest.getRequestURI();
        
        // Skip validation for excluded paths
        if (isExcludedPath(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        // Extract client ID from header
        String clientId = httpRequest.getHeader("X-Client-ID");
        
        if (clientId == null || clientId.trim().isEmpty()) {
            sendErrorResponse(httpResponse, 
                "X-Client-ID header is required. Please include a valid client ID in the request header. " +
                "Default client IDs: web-app, mobile-app, api-client, desktop-app", 
                HttpStatus.BAD_REQUEST);
            return;
        }

        // Validate client ID
        try {
            clientValidationService.validateClientId(clientId);
        } catch (UnauthorizedException e) {
            sendErrorResponse(httpResponse, 
                "Invalid client ID: " + clientId + ". Please use a valid client ID. " +
                "Contact your administrator if you need a new client application registered.", 
                HttpStatus.UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isExcludedPath(String path) {
        // Check exact matches
        if (EXCLUDED_PATHS.contains(path)) {
            return true;
        }
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private void sendErrorResponse(HttpServletResponse response, String message, HttpStatus status) 
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d,\"timestamp\":\"%s\"}",
            status.getReasonPhrase(), message, status.value(), java.time.Instant.now()
        ));
    }

}
