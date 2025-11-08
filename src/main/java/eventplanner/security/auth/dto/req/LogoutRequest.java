package eventplanner.security.auth.dto.req;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

/**
 * Request DTO for logout endpoint.
 * Requires explicit confirmation to prevent accidental sign-outs.
 * Logs out from the current authenticated device.
 */
@Data
public class LogoutRequest {

    /**
     * Must be true to confirm logout action.
     * This prevents accidental revocation of the session.
     */
    @AssertTrue(message = "Confirmation required. Set 'confirm' to true to logout from this device.")
    private Boolean confirm;
}

