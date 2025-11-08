package eventplanner.security.filters;

import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSession;
import eventplanner.security.auth.repository.UserSessionRepository;
import eventplanner.security.auth.service.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Filter to validate deviceId header and session for authenticated requests.
 * 
 * This filter runs after JwtAuthenticationFilter and validates:
 * 1. X-Device-ID header is present
 * 2. Session exists for user (from JWT) + deviceId (from header)
 * 3. Session is valid (not revoked, not expired)
 * 
 * Public endpoints (register, login) are skipped as they don't require deviceId.
 */
@Component
@Order(3) // After JWT filter (which is typically Order 2 or implicit)
@Slf4j
@RequiredArgsConstructor
public class DeviceValidationFilter extends OncePerRequestFilter {

    private final UserSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    // Endpoints that don't require deviceId validation
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/v1/auth/register",         // Public - issues tokens on register in current design
        "/api/v1/auth/login",            // Public - issues deviceId here
        "/api/v1/auth/refresh-token",    // Public - validated via refresh token, not JWT
        "/api/v1/auth/health",           // Health check
        "/api/v1/auth/forgot-password",  // Password recovery
        "/api/v1/auth/reset-password",   // Password reset
        "/api/v1/auth/verify-email",     // Email verification
        "/api/v1/auth/validate-token"    // Token validation (public endpoint)
    );
    // Note: Refresh token endpoint is public in current configuration; deviceId is not required there

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // Skip for public endpoints
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Only validate for authenticated endpoints
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            // Not authenticated - let other filters handle it
            filterChain.doFilter(request, response);
            return;
        }
        
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        UserAccount user = principal.getUser();
        
        // Extract X-Device-ID header
        String headerDeviceId = request.getHeader("X-Device-ID");
        
        // Validate header present
        if (!StringUtils.hasText(headerDeviceId)) {
            log.warn("Missing X-Device-ID header for authenticated request: {} from user: {}", 
                    requestURI, user.getId());
            sendErrorResponse(response, "X-Device-ID header is required", HttpStatus.BAD_REQUEST, requestURI);
            return;
        }
        
        // Look up session by user (from JWT) + deviceId (from header)
        Optional<UserSession> sessionOpt = sessionRepository.findByUserAndDeviceId(user, headerDeviceId.trim());
        
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found for user: {} with deviceId: {} on request: {}", 
                    user.getId(), headerDeviceId, requestURI);
            sendErrorResponse(response, "Session not found", HttpStatus.UNAUTHORIZED, requestURI);
            return;
        }
        
        UserSession session = sessionOpt.get();
        
        // Validate session is valid (not revoked, not expired)
        if (!session.isValid()) {
            String reason = session.isRevoked() ? "revoked" : "expired";
            log.warn("Session {} for user: {} with deviceId: {} on request: {}", 
                    reason, user.getId(), headerDeviceId, requestURI);
            sendErrorResponse(response, "Session expired or revoked", HttpStatus.UNAUTHORIZED, requestURI);
            return;
        }
        
        // Update last seen timestamp
        session.updateLastSeen();
        sessionRepository.save(session);
        
        log.debug("Device validation successful for user: {} with deviceId: {} on request: {}", 
                user.getId(), headerDeviceId, requestURI);
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private void sendErrorResponse(HttpServletResponse response, String message, HttpStatus status, String path) 
        throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
            "status", status.value(),
            "error", status.getReasonPhrase(),
            "message", message,
            "path", path,
            "timestamp", java.time.Instant.now().toString()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
