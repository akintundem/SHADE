package eventplanner.security.authorization.rbac;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

/**
 * In-memory representation of a permission entry from RBAC_policy.yml.
 */
@Value
@Builder
public class RbacPermissionDefinition {
    String name;
    RbacScope scope;
    List<String> resources;
    String description;
    List<String> conditions;
    AbacRules abac;

    public List<String> getResources() {
        return resources == null ? List.of() : resources;
    }

    public List<String> getConditions() {
        return conditions == null ? List.of() : conditions;
    }

    public AbacRules getAbac() {
        return abac == null ? AbacRules.DEFAULT : abac;
    }

    @Value
    @Builder
    public static class AbacRules {
        public static final AbacRules DEFAULT = AbacRules.builder().build();

        @Builder.Default
        boolean isAuthenticated = false;

        @Builder.Default
        boolean ownsScope = false;

        @Builder.Default
        boolean isMember = false;
    }
}
