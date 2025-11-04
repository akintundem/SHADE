package eventplanner.security.filters;

import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.domain.enums.UserStatus;
import eventplanner.security.util.JwtValidationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidationUtil jwtValidationUtil;
    private final UserAccountRepository userAccountRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtValidationUtil jwtValidationUtil, 
                                   UserAccountRepository userAccountRepository,
                                   ObjectMapper objectMapper) {
        this.jwtValidationUtil = jwtValidationUtil;
        this.userAccountRepository = userAccountRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();
        
        // If no Authorization header, continue chain (will be caught by security config if endpoint requires auth)
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            // Validate token
            if (!jwtValidationUtil.validateToken(token)) {
                log.warn("Invalid JWT token for request: {}", requestURI);
                sendUnauthorizedResponse(response, "Invalid or expired token", requestURI);
                return;
            }

            Claims claims = jwtValidationUtil.getClaimsFromToken(token);
            String subject = claims.getSubject();
            if (!StringUtils.hasText(subject)) {
                log.warn("JWT token missing subject for request: {}", requestURI);
                sendUnauthorizedResponse(response, "Invalid token format", requestURI);
                return;
            }

            UUID userId;
            try {
                userId = UUID.fromString(subject);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid user ID format in JWT token: {}", subject);
                sendUnauthorizedResponse(response, "Invalid token format", requestURI);
                return;
            }
            
            // Try to find user with a small delay to allow for transaction commit
            Optional<UserAccount> userOpt = userAccountRepository.findById(userId);
            if (userOpt.isEmpty()) {
                // Try again after a small delay
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                userOpt = userAccountRepository.findById(userId);
            }
            
            if (userOpt.isEmpty()) {
                log.warn("User not found for token subject: {} on request: {}", userId, requestURI);
                sendUnauthorizedResponse(response, "User not found", requestURI);
                return;
            }

            UserAccount user = userOpt.get();
            // Check if user is still active
            if (user.getStatus() != UserStatus.ACTIVE) {
                log.warn("Inactive user attempted access: {} on request: {}", userId, requestURI);
                sendUnauthorizedResponse(response, "User account is inactive", requestURI);
                return;
            }

            UserPrincipal principal = new UserPrincipal(user);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("Successfully authenticated user: {} for request: {}", userId, requestURI);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid JWT token format for request: {} - {}", requestURI, ex.getMessage());
            sendUnauthorizedResponse(response, "Invalid token format", requestURI);
            return;
        } catch (Exception ex) {
            log.error("JWT authentication failed for request: {} - {}", requestURI, ex.getMessage(), ex);
            sendUnauthorizedResponse(response, "Authentication failed", requestURI);
            return;
        }

        filterChain.doFilter(request, response);
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String message, String path) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "status", HttpServletResponse.SC_UNAUTHORIZED,
                "error", "Unauthorized",
                "message", message,
                "path", path
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
