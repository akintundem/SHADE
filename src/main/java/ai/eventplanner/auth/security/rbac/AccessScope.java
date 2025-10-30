package ai.eventplanner.auth.security.rbac;

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
