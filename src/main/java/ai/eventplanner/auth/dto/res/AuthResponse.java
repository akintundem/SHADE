package ai.eventplanner.auth.dto.res;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    String message;
    SecureUserResponse user;
    String accessToken;
    String refreshToken;
    String tokenType;
}
