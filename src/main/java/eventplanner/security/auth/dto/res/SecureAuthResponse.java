package eventplanner.security.auth.dto.res;

import lombok.Builder;
import lombok.Value;

/**
 * Secure Auth Response DTO that excludes sensitive user identifiers.
 * Includes onboardingRequired flag to indicate if user needs to complete profile onboarding.
 */
@Value
@Builder
public class SecureAuthResponse {
    String message;
    SecureUserResponse user;
    String accessToken;
    String refreshToken;
    String tokenType;
    String deviceId; // Server-issued device identifier for session validation
    boolean onboardingRequired; // Indicates if user needs to complete profile onboarding
}
