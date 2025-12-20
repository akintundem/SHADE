package eventplanner.security.auth.dto.req;

import eventplanner.common.domain.enums.LanguagePreference;
import eventplanner.common.domain.enums.ThemePreference;
import eventplanner.common.domain.enums.VisibilityLevel;
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
}
