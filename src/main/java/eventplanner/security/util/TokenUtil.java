package eventplanner.security.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for generating and hashing tokens.
 * Generates cryptographically secure random tokens and provides hashing
 * for safe storage in the database.
 */
public final class TokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenUtil() {
    }

    /**
     * Generates a cryptographically secure random token.
     * This token should be sent to users (e.g., via email) but never stored in the database.
     *
     * @return A URL-safe Base64-encoded random token
     */
    public static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hashes a token using SHA-256 for safe storage in the database.
     * Always hash tokens before storing them - never store raw tokens.
     *
     * @param token The raw token to hash
     * @return SHA-256 hash of the token as a hexadecimal string
     */
    public static String hashToken(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Convenience method that generates a token and returns both the raw token and its hash.
     * Useful when creating invites that need both the token to send and the hash to store.
     *
     * @return A TokenPair containing both the raw token and its hash
     */
    public static TokenPair generateAndHash() {
        String token = generateToken();
        String hash = hashToken(token);
        return new TokenPair(token, hash);
    }

    /**
     * Immutable pair containing a raw token and its hash.
     */
    public static final class TokenPair {
        private final String token;
        private final String hash;

        private TokenPair(String token, String hash) {
            this.token = token;
            this.hash = hash;
        }

        public String getToken() {
            return token;
        }

        public String getHash() {
            return hash;
        }
    }
}
