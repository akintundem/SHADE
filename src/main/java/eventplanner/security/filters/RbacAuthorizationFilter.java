package eventplanner.security.filters;

import eventplanner.security.authorization.rbac.AccessScope;
import eventplanner.security.authorization.rbac.PermissionCheck;
import eventplanner.security.authorization.rbac.PermissionDescriptor;
import eventplanner.security.authorization.rbac.PermissionRegistry;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Filter that enforces RBAC policies using the configured permission registry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RbacAuthorizationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_ENDPOINT_PREFIXES = Set.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh-token",
        "/api/v1/auth/verify-email",
        "/api/v1/auth/reset-password",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/validate-token",
        "/api/v1/auth/health",
        "/api/v1/weather",
        "/health",
        "/actuator/health",
        "/actuator/info",
        "/error",
        "/favicon.ico",
        "/css/",
        "/js/",
        "/images/"
    );

    private final PermissionRegistry permissionRegistry;
    private final AuthorizationService authorizationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (isPublicEndpoint(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal user)) {
            log.warn("Blocking unauthenticated access to {} {}", method, uri);
            respondUnauthorized(response);
            return;
        }

        Optional<PermissionCheck> permission = permissionRegistry.resolve(request);
        if (permission.isEmpty()) {
            log.debug("RBAC policy missing for {} {} - denying access", method, uri);
            respondForbidden(response, "Access denied", "No RBAC rule covers this endpoint");
            return;
        }

        PermissionCheck check = permission.get();
        PermissionDescriptor descriptor = check.descriptor();

        if (descriptor == null || descriptor.permission() == null || descriptor.permission().isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (descriptor.scope() == AccessScope.PUBLIC) {
            filterChain.doFilter(request, response);
            return;
        }

        if (descriptor.allowAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (descriptor.allowOwner() && authorizationService.isOwner(user, check)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authorizationService.hasPermission(user, check)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("RBAC denied {} {} for user {}", method, uri, user.getId());
        respondForbidden(response, "Access denied", "Insufficient permissions");
    }

    private boolean isPublicEndpoint(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        return PUBLIC_ENDPOINT_PREFIXES.stream().anyMatch(uri::startsWith) || "/".equals(uri);
    }

    private void respondUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
    }

    private void respondForbidden(HttpServletResponse response, String error, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
