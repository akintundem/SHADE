package eventplanner.security.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    @Size(max = 120)
    private String deviceId;

    @Size(max = 120)
    private String clientId;
}
