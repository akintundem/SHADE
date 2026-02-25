package eventplanner.security.auth.service;

/**
 * Identity provider user lifecycle (Auth0 or other OIDC).
 * Implementations may no-op when config is missing.
 */
public interface IdpUserService {

    /**
     * Delete the user from the IdP. No-op if not configured.
     */
    void deleteUser(String authSub, String emailFallback);

    /**
     * Revoke all sessions/tokens for the user. No-op if not configured.
     */
    void signOutUser(String authSub, String emailFallback);

    /**
     * Update IdP profile (name, username, phone). No-op if not configured.
     */
    void updateUserProfile(String authSub,
                           String emailFallback,
                           String name,
                           String preferredUsername,
                           String phoneNumber);

    /**
     * Mark the user's email as verified in the IdP. No-op if not configured.
     * Used after JIT signup so the user can immediately log in without waiting
     * for a verification email (email verification enforcement is delegated to
     * application-level policy, not the IdP when using this flow).
     */
    void markEmailVerified(String authSub);
}
