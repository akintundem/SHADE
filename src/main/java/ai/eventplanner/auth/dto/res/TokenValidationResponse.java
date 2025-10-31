package ai.eventplanner.auth.dto.res;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TokenValidationResponse {
    boolean valid;
    String error;
    SecureUserResponse user;

    public static TokenValidationResponse valid(SecureUserResponse user) {
        return TokenValidationResponse.builder()
            .valid(true)
            .user(user)
            .build();
    }

    public static TokenValidationResponse invalid(String error) {
        return TokenValidationResponse.builder()
            .valid(false)
            .error(error)
            .build();
    }
}
