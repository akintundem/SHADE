package ai.eventplanner.auth.security.rbac;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * POJO representation of the RBAC configuration file.
 */
@Data
public class RbacConfig {

    private List<RouteConfig> routes = List.of();
    private RoleConfig roles = new RoleConfig();

    @Data
    public static class RouteConfig {
        private String name;
        private String pattern;
        private String resourceIdGroup;
        private Map<String, MethodConfig> methods = Collections.emptyMap();
    }

    @Data
    public static class MethodConfig {
        private String permission;
        private AccessScope scope = AccessScope.SYSTEM;
        private boolean allowOwner = false;
        private boolean allowAuthenticated = false;
    }

    @Data
    public static class RoleConfig {
        private Map<String, List<String>> system = Collections.emptyMap();
        private Map<String, List<String>> organization = Collections.emptyMap();
        private Map<String, List<String>> event = Collections.emptyMap();
    }
}
