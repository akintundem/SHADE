package eventplanner.security.util;

import java.util.Locale;

/**
 * Utility functions for validating and normalizing authentication inputs.
 */
public final class AuthValidationUtil {

    private AuthValidationUtil() {
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);

        if (!normalized.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (normalized.length() > 254) {
            throw new IllegalArgumentException("Email address too long");
        }

        return normalized;
    }

    public static String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Normalizes usernames/handles for consistent storage and uniqueness checks.
     * - trims whitespace
     * - strips a leading '@'
     * - lowercases
     */
    public static String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String normalized = username.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        normalized = normalized.trim().toLowerCase(Locale.ROOT);

        // Basic extra safety: prevent double dots which often cause confusion in handles.
        if (normalized.contains("..")) {
            throw new IllegalArgumentException("Username cannot contain consecutive dots");
        }
        return normalized;
    }
}
