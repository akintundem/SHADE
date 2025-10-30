package ai.eventplanner.auth.security.rbac;

import lombok.Builder;

/**
 * Permission metadata for a specific HTTP method.
 */
@Builder
public record PermissionDescriptor(
        String permission,
        AccessScope scope,
        boolean allowOwner,
        boolean allowAuthenticated
) {

    public boolean requiresResolution() {
        return scope != null && scope.requiresResource();
    }

    public static PermissionDescriptorBuilder builder() {
        return new PermissionDescriptorBuilder()
            .scope(AccessScope.SYSTEM)
            .allowOwner(false)
            .allowAuthenticated(false);
    }
}
