package ai.eventplanner.auth.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    String message;
    UserResponse user;
    String accessToken;
    String refreshToken;
    String tokenType;
}
