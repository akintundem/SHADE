package eventplanner.security.auth.dto.req;

import eventplanner.security.auth.enums.VisibilityLevel;
import lombok.Data;

@Data
public class PrivacySettingsUpdateRequest {
    private VisibilityLevel profileVisibility;
    private VisibilityLevel eventParticipationVisibility;
    private Boolean searchVisibility;
    private Boolean showInEventDirectory;
}
