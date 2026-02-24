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
     *
     * <p>Handles:
     * <ul>
     *   <li>Standard OIDC: {@code email_verified} claim present and true</li>
     *   <li>AWS Cognito access tokens: {@code token_use == "access"}</li>
     *   <li>Auth0 access tokens: issuer contains ".auth0.com" or ".us.auth0.com"
     *       (Auth0 access tokens are opaque from the email-verification perspective;
     *       email verification is enforced at the Auth0 application level)</li>
     * </ul>
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
        // AWS Cognito access tokens
        String tokenUse = jwt.getClaimAsString("token_use");
        if ("access".equals(tokenUse)) {
            return true;
        }
        // Auth0 access tokens: issuer contains auth0.com
        String issuer = jwt.getClaimAsString("iss");
        if (StringUtils.hasText(issuer) && issuer.contains(".auth0.com")) {
            return true;
        }
        return false;
    }
}
