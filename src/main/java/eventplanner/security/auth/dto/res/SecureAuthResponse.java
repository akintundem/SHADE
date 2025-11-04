package eventplanner.security.auth.dto.res;

import lombok.Builder;
import lombok.Value;

/**
 * Secure Auth Response DTO that excludes sensitive user identifiers
 */
@Value
@Builder
public class SecureAuthResponse {
    String message;
    SecureUserResponse user;
    String accessToken;
    String refreshToken;
    String tokenType;
}
