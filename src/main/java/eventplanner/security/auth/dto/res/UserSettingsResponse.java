package eventplanner.security.auth.dto.res;

import eventplanner.common.domain.enums.LanguagePreference;
import eventplanner.common.domain.enums.ThemePreference;
import eventplanner.common.domain.enums.VisibilityLevel;
import eventplanner.security.auth.dto.LocationDto;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserSettingsResponse {
    String bio;
    LocationDto location;
    LanguagePreference preferredLanguage;
    VisibilityLevel profileVisibility;
    VisibilityLevel eventParticipationVisibility;
    boolean searchVisibility;
    ThemePreference themePreference;
    boolean emailNotificationsEnabled;
    boolean pushNotificationsEnabled;
    boolean eventInvitationsEnabled;
    boolean eventUpdatesEnabled;
    boolean eventRemindersEnabled;
    boolean rsvpNotificationsEnabled;
    boolean commentNotificationsEnabled;
    boolean collaborationRequestsEnabled;
    boolean weeklyDigestEnabled;
    boolean activityFeedNotificationsEnabled;
    boolean autoAcceptInvitations;
    boolean exportEventDataEnabled;
    boolean mfaEnabled;
    Integer reminderTimingMinutes;
    boolean showInEventDirectory;
    boolean smsNotificationsEnabled;
}
