package eventplanner.security.auth.dto.req;

import eventplanner.common.domain.enums.VisibilityLevel;
import lombok.Data;

@Data
public class PrivacySettingsUpdateRequest {
    private VisibilityLevel profileVisibility;
    private VisibilityLevel eventParticipationVisibility;
    private Boolean searchVisibility;
    private Boolean showInEventDirectory;
}
