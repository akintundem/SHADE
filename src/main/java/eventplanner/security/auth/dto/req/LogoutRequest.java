package eventplanner.security.auth.dto.req;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

/**
 * Request DTO for logout endpoint.
 * Requires explicit confirmation to prevent accidental sign-outs from all devices.
 */
@Data
public class LogoutRequest {

    /**
     * Must be true to confirm logout action.
     * This prevents accidental revocation of all sessions.
     */
    @AssertTrue(message = "Confirmation required. Set 'confirm' to true to logout from all devices.")
    private Boolean confirm;
}

