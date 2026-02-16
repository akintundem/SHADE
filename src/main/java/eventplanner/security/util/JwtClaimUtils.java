package eventplanner.security.util;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Shared JWT claim helpers — eliminates duplication between
 * CognitoJwtAuthenticationConverter and AuthInfoController.
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
     * Convenience: is email verified, with access-token fallback.
     * When the token is an access token (no email_verified claim), returns true
     * because Cognito only issues access tokens for verified emails.
     * SECURITY: This assumes IdP contract (Cognito) — enforce contract tests and fail-safe
     * if claim model changes (e.g. access token without verified email).
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
