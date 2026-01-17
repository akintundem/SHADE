package eventplanner.security.auth.dto.req;

import eventplanner.security.auth.enums.LanguagePreference;
import eventplanner.security.auth.enums.ThemePreference;
import eventplanner.security.auth.enums.VisibilityLevel;
import eventplanner.security.auth.dto.LocationDto;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

@Data
public class UserSettingsUpdateRequest {

    @Size(max = 500)
    private String bio;

    @Valid
    private LocationDto location;

    private LanguagePreference preferredLanguage;

    private VisibilityLevel profileVisibility;

    private VisibilityLevel eventParticipationVisibility;

    private Boolean searchVisibility;

    private ThemePreference themePreference;

    private Boolean emailNotificationsEnabled;

    private Boolean pushNotificationsEnabled;

    private Boolean eventInvitationsEnabled;

    private Boolean eventUpdatesEnabled;

    private Boolean eventRemindersEnabled;

    private Boolean rsvpNotificationsEnabled;

    private Boolean commentNotificationsEnabled;

    private Boolean collaborationRequestsEnabled;

    private Boolean weeklyDigestEnabled;

    private Boolean activityFeedNotificationsEnabled;

    private Boolean autoAcceptInvitations;

    private Boolean exportEventDataEnabled;

    private Boolean mfaEnabled;

    private Integer reminderTimingMinutes;

    private Boolean showInEventDirectory;

    private Boolean smsNotificationsEnabled;
}
