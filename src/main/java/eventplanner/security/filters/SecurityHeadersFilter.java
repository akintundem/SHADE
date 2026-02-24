package eventplanner.security.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to add security headers to all responses
 * Automatically detects web browsers vs mobile apps and applies appropriate headers
 * - Universal headers: Applied to all requests (web + mobile)
 * - Web-specific headers: Only applied to web browser requests
 */
@Component
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

        // SECURITY: Apply web security headers unconditionally — User-Agent is
        // trivially spoofed so it must not be used as a security gate.
        // These headers are harmless for API/mobile clients and critical for browsers.
        addWebSpecificHeaders(httpResponse);

        // HSTS (only for HTTPS)
        if (httpRequest.isSecure()) {
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }

        chain.doFilter(request, response);
    }
    
    /**
     * Add web-specific security headers
     */
    private void addWebSpecificHeaders(HttpServletResponse response) {
        // Web-only security headers
        response.setHeader("X-Frame-Options", "DENY");
        // X-XSS-Protection is deprecated but still useful for older browsers
        response.setHeader("X-XSS-Protection", "1; mode=block");
        // Permissions-Policy restricts browser features
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=(), usb=()");
        
        // Content Security Policy (web-only) - OWASP A03:2021 Injection Prevention
        // Note: 'unsafe-inline' for styles is often needed for frameworks.
        // For API-only backends, this is less critical as responses are JSON.
        // If serving HTML pages, consider using nonces or hashes instead of unsafe-inline.
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self'; " +  // Removed unsafe-inline and unsafe-eval for security
            "style-src 'self' 'unsafe-inline'; " +  // Inline styles often needed for email templates
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' https:; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'; " +
            "upgrade-insecure-requests");
    }
}
