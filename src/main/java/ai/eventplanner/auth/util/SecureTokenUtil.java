package ai.eventplanner.auth.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for generating cryptographically secure random tokens.
 */
public final class SecureTokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokenUtil() {
    }

    public static String generateSecureToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
