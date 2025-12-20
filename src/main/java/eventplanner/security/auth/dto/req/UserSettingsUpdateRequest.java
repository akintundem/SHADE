package eventplanner.security.auth.dto.req;

import eventplanner.common.domain.enums.ThemePreference;
import eventplanner.common.domain.enums.VisibilityLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserSettingsUpdateRequest {

    @Size(max = 500)
    private String bio;

    @Size(max = 200)
    private String location;

    @Size(max = 60)
    private String timeZone;

    @Size(max = 12)
    private String preferredLanguage;

    private VisibilityLevel profileVisibility;

    private Boolean searchVisibility;

    private VisibilityLevel eventParticipationVisibility;

    private ThemePreference themePreference;

    private Boolean emailNotificationsEnabled;

    private Boolean smsNotificationsEnabled;

    private Boolean pushNotificationsEnabled;

    private Boolean eventInvitationsEnabled;

    private Boolean eventUpdatesEnabled;

    private Boolean eventRemindersEnabled;

    @Min(5)
    @Max(10080)
    private Integer reminderTimingMinutes;

    private Boolean rsvpNotificationsEnabled;

    private Boolean commentNotificationsEnabled;

    private Boolean collaborationRequestsEnabled;

    private Boolean weeklyDigestEnabled;

    private Boolean activityFeedNotificationsEnabled;

    private Boolean autoAcceptInvitations;

    private Boolean showInEventDirectory;

    private Boolean exportEventDataEnabled;
}
