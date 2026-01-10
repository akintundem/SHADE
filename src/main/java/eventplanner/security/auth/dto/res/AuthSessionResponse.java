package eventplanner.security.auth.dto.res;

import lombok.Builder;
import lombok.Value;

/**
 * Wrapper returned after login so the client can tell whether this is
 * a freshly provisioned account and if onboarding must run.
 */
@Value
@Builder
public class AuthSessionResponse {
    SecureUserResponse user;
    boolean onboardingRequired;
}
