package eventplanner.security.util;

import java.util.Locale;

/**
 * Utility functions for validating and normalizing authentication inputs.
 */
public final class AuthValidationUtil {

    private AuthValidationUtil() {
    }

    public static void validatePasswordMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        validatePasswordStrength(password);
    }

    public static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        if (password.length() > 128) {
            throw new IllegalArgumentException("Password too long (max 128 characters)");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
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
}
