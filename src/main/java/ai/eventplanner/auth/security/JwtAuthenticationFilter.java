package ai.eventplanner.auth.security;

import ai.eventplanner.auth.entity.UserAccount;
import ai.eventplanner.auth.repo.UserAccountRepository;
import ai.eventplanner.auth.service.UserPrincipal;
import ai.eventplanner.common.security.JwtValidationUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    private final JwtValidationUtil jwtValidationUtil;
    private final UserAccountRepository userAccountRepository;

    public JwtAuthenticationFilter(JwtValidationUtil jwtValidationUtil, UserAccountRepository userAccountRepository) {
        this.jwtValidationUtil = jwtValidationUtil;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtValidationUtil.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtValidationUtil.getClaimsFromToken(token);
            String subject = claims.getSubject();
            if (!StringUtils.hasText(subject)) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(subject);
            Optional<UserAccount> userOpt = userAccountRepository.findById(userId);
            if (userOpt.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            UserAccount user = userOpt.get();
            // Check if user is still active
            if (!"ACTIVE".equals(user.getStatus())) {
                filterChain.doFilter(request, response);
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
        } catch (IllegalArgumentException ex) {
        } catch (Exception ex) {
            // Don't expose internal error details to client
        }

        filterChain.doFilter(request, response);
    }
}
