package ai.eventplanner.auth.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to add security headers to all responses
 * Automatically detects web browsers vs mobile apps and applies appropriate headers
 * - Universal headers: Applied to all requests (web + mobile)
 * - Web-specific headers: Only applied to web browser requests
 */
@Component
@Order(2)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Universal security headers (work for both web and mobile)
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Remove server information
        httpResponse.setHeader("Server", "");
        
        // Cache control for sensitive endpoints (important for mobile apps)
        String requestPath = httpRequest.getRequestURI();
        if (requestPath.contains("/auth/") || requestPath.contains("/api/")) {
            httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Expires", "0");
        }

        // Web-specific headers (only apply to web browsers)
        if (isWebBrowserRequest(httpRequest)) {
            addWebSpecificHeaders(httpResponse);
        }
        
        // HSTS (only for HTTPS)
        if (httpRequest.isSecure()) {
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }

        chain.doFilter(request, response);
    }
    
    /**
     * Check if request is from a web browser (not mobile app)
     */
    private boolean isWebBrowserRequest(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return false;
        }
        
        // Mobile app user agents typically contain app-specific identifiers
        String lowerUserAgent = userAgent.toLowerCase();
        
        // Common mobile app patterns
        boolean isMobileApp = lowerUserAgent.contains("okhttp") ||  // Android HTTP client
                             lowerUserAgent.contains("alamofire") || // iOS HTTP client
                             lowerUserAgent.contains("cfnetwork") || // iOS networking
                             lowerUserAgent.contains("mobile app") ||
                             lowerUserAgent.contains("android app") ||
                             lowerUserAgent.contains("ios app");
        
        // If it's clearly a mobile app, don't apply web headers
        if (isMobileApp) {
            return false;
        }
        
        // If it's a web browser, apply web headers
        return lowerUserAgent.contains("mozilla") || 
               lowerUserAgent.contains("chrome") || 
               lowerUserAgent.contains("safari") || 
               lowerUserAgent.contains("firefox") ||
               lowerUserAgent.contains("edge");
    }
    
    /**
     * Add web-specific security headers
     */
    private void addWebSpecificHeaders(HttpServletResponse response) {
        // Web-only security headers
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        
        // Content Security Policy (web-only)
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' https:; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'");
    }
}
