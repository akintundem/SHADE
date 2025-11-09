package eventplanner.security.authorization.rbac;

/**
 * Scope dimension for RBAC permissions as defined in the policy document.
 */
public enum RbacScope {
    SYSTEM,
    ORGANIZATION,
    EVENT,
    PUBLIC;

    public static RbacScope from(String raw) {
        if (raw == null || raw.isBlank()) {
            return PUBLIC;
        }
        String normalized = raw.trim().toUpperCase();
        for (RbacScope scope : values()) {
            if (scope.name().equals(normalized)) {
                return scope;
            }
        }
        return PUBLIC;
    }
}
