package eventplanner.security.auth.dto.res;

import eventplanner.common.domain.enums.ThemePreference;
import eventplanner.common.domain.enums.VisibilityLevel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserSettingsResponse {
    String bio;
    String location;
    String timeZone;
    String preferredLanguage;
    VisibilityLevel profileVisibility;
    boolean searchVisibility;
    VisibilityLevel eventParticipationVisibility;
    ThemePreference themePreference;
    boolean emailNotificationsEnabled;
    boolean smsNotificationsEnabled;
    boolean pushNotificationsEnabled;
    boolean eventInvitationsEnabled;
    boolean eventUpdatesEnabled;
    boolean eventRemindersEnabled;
    Integer reminderTimingMinutes;
    boolean rsvpNotificationsEnabled;
    boolean commentNotificationsEnabled;
    boolean collaborationRequestsEnabled;
    boolean weeklyDigestEnabled;
    boolean activityFeedNotificationsEnabled;
    boolean autoAcceptInvitations;
    boolean showInEventDirectory;
    boolean exportEventDataEnabled;
    boolean mfaEnabled;
}
