package eventplanner.security.authorization.rbac;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Provides permission lookups for incoming HTTP requests.
 */
@Component
@Slf4j
public class PermissionRegistry {

    private final RbacConfigLoader configLoader;
    @Getter
    private RolePermissionMatrix rolePermissionMatrix;
    private List<RoutePermission> routePermissions;

    public PermissionRegistry(RbacConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @PostConstruct
    void init() {
        PermissionMatcher matcher = new PermissionMatcher();
        RbacConfig config = configLoader.load();
        this.rolePermissionMatrix = RolePermissionMatrix.from(config.getRoles(), matcher);
        this.routePermissions = config.getRoutes().stream()
            .map(RoutePermission::from)
            .toList();
    }

    public Optional<PermissionCheck> resolve(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return Optional.empty();
        }
        String method = request.getMethod().toUpperCase(Locale.US);
        String uri = request.getRequestURI();

        return routePermissions.stream()
            .map(route -> route.resolve(request))
            .flatMap(Optional::stream)
            .findFirst()
            .map(check -> {
                log.debug("Resolved RBAC permission '{}' for {} {}", 
                    check.descriptor().permission(), method, uri);
                return check;
            });
    }
}
