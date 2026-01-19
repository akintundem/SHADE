package eventplanner.security.auth.dto.res;

import eventplanner.security.auth.enums.LanguagePreference;
import eventplanner.security.auth.enums.ThemePreference;
import eventplanner.security.auth.enums.VisibilityLevel;
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
