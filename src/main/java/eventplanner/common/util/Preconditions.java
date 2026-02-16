package eventplanner.common.util;

import eventplanner.common.exception.exceptions.ForbiddenException;
import eventplanner.common.exception.exceptions.UnauthorizedException;
import eventplanner.security.auth.service.UserPrincipal;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Shared precondition checks that were duplicated 50+ times across controllers and services.
 * Eliminates boilerplate null-checks and common guard patterns.
 * Uses custom exceptions (UnauthorizedException, ForbiddenException) for consistent RFC 7807 responses.
 */
public final class Preconditions {

    private Preconditions() {}

    // ---- Principal validation (replaces ~50 inline null checks across controllers) ----

    /**
     * Require a non-null authenticated principal — throws 401 UNAUTHORIZED.
     */
    public static void requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
    }

    /**
     * Require a non-null authenticated principal with a user ID.
     */
    public static void requireAuthenticatedWithId(UserPrincipal principal) {
        requireAuthenticated(principal);
        if (principal.getId() == null) {
            throw new UnauthorizedException("Authentication required");
        }
    }

    /**
     * Require that the authenticated principal matches the given user ID.
     * Throws 403 Forbidden if they differ.
     */
    public static void requireSameUser(UserPrincipal principal, UUID userId) {
        requireAuthenticatedWithId(principal);
        if (!principal.getId().equals(userId)) {
            throw new ForbiddenException("Cannot access another user's data");
        }
    }

    // ---- Configuration validation (replaces 3+ copies of requireConfigured) ----

    /**
     * Require a non-null Boolean config property.
     */
    public static boolean requireConfigured(Boolean value, String propertyName) {
        if (value == null) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
        return value;
    }

    /**
     * Require a non-blank String config property.
     */
    public static String requireConfigured(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
        return value;
    }
}
