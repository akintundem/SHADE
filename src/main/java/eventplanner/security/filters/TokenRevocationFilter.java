package eventplanner.security.filters;

import eventplanner.security.auth.service.TokenRevocationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects requests that carry a JWT whose JTI has been added to the revocation
 * blocklist (e.g., after the user calls {@code POST /api/v1/auth/logout}).
 *
 * <p>This filter runs after {@code BearerTokenAuthenticationFilter} so that the
 * {@link SecurityContextHolder} is already populated when we inspect the token.
 */
@Component
public class TokenRevocationFilter extends OncePerRequestFilter {

    private final TokenRevocationService tokenRevocationService;

    public TokenRevocationFilter(TokenRevocationService tokenRevocationService) {
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String jti = jwt.getId(); // "jti" claim
            if (jti != null && tokenRevocationService.isRevoked(jti)) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                    "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Token has been revoked\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
