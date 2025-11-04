package eventplanner.security.authorization.rbac;

import java.util.UUID;

/**
 * Resolved permission for a specific request including any contextual identifiers.
 */
public record PermissionCheck(
        PermissionDescriptor descriptor,
        UUID resourceId,
        String resourceIdValue) {
}
