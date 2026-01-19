package eventplanner.security.auth.dto.req;

import lombok.Data;

@Data
public class SecuritySettingsUpdateRequest {
    private Boolean mfaEnabled;
    private Boolean autoAcceptInvitations;
    private Boolean exportEventDataEnabled;
}
