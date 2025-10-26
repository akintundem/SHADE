package ai.eventplanner.auth.dto;

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
