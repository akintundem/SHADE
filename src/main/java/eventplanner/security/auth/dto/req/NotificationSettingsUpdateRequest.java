package eventplanner.security.auth.dto.req;

import lombok.Data;

@Data
public class NotificationSettingsUpdateRequest {
    private Boolean emailNotificationsEnabled;
    private Boolean pushNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Boolean eventInvitationsEnabled;
    private Boolean eventUpdatesEnabled;
    private Boolean eventRemindersEnabled;
    private Boolean rsvpNotificationsEnabled;
    private Boolean commentNotificationsEnabled;
    private Boolean collaborationRequestsEnabled;
    private Boolean weeklyDigestEnabled;
    private Boolean activityFeedNotificationsEnabled;
    private Integer reminderTimingMinutes;
}
