package eventplanner.security.util;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Shared JWT claim helpers for OIDC (e.g. Auth0) JWTs.
 */
public final class JwtClaimUtils {

    private JwtClaimUtils() {}

    /**
     * Check whether the JWT's {@code email_verified} claim is true.
     * Handles both Boolean and String representations of the claim.
     */
    public static boolean isEmailVerified(Jwt jwt) {
        Boolean verified = jwt.getClaimAsBoolean("email_verified");
        if (verified != null) {
            return verified;
        }
        String rawValue = jwt.getClaimAsString("email_verified");
        if (StringUtils.hasText(rawValue)) {
            return "true".equalsIgnoreCase(rawValue);
        }
        return false;
    }

    /**
     * Convenience: is email verified, with optional access-token fallback for IdPs
     * that omit email_verified on access tokens.
     */
    public static boolean isEmailVerifiedOrAccessToken(Jwt jwt) {
        Boolean verified = jwt.getClaimAsBoolean("email_verified");
        if (verified != null) {
            return verified;
        }
        String rawValue = jwt.getClaimAsString("email_verified");
        if (StringUtils.hasText(rawValue)) {
            return "true".equalsIgnoreCase(rawValue);
        }
        String tokenUse = jwt.getClaimAsString("token_use");
        return "access".equals(tokenUse);
    }
}
