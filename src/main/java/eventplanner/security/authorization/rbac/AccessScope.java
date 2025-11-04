package eventplanner.security.authorization.rbac;

/**
 * Scope for permission evaluation.
 */
public enum AccessScope {
    SYSTEM,
    ORGANIZATION,
    EVENT,
    PUBLIC;

    public boolean requiresResource() {
        return this == ORGANIZATION || this == EVENT;
    }
}
