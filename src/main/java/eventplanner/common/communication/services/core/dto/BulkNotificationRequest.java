package eventplanner.common.communication.services.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Internal DTO for sending bulk notifications.
 * Used by BulkNotificationService for internal operations only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationRequest {

    private NotificationTarget.TargetType targetType;

    /**
     * Event ID (required for event-related targets like EVENT_ATTENDEES, EVENT_COLLABORATORS)
     */
    private UUID eventId;

    private String title;

    /**
     * Body/content of the notification
     * For push notifications, this is the notification body
     * For emails, this may be used as template variable or plain text
     */
    private String body;

    /**
     * Additional data/metadata for the notification
     * For push notifications, this becomes the data payload
     * For emails, these are template variables
     */
    @Builder.Default
    private Map<String, String> data = new HashMap<>();

    /**
     * Email template ID (required for email notifications)
     */
    private String templateId;

    /**
     * From address for email notifications
     */
    private String from;

    /**
     * Additional parameters for target resolution
     * Examples:
     * - For EVENT_ATTENDEES: {"rsvpStatus": "CONFIRMED"}
     * - For ALL_USERS: {"page": 0, "size": 1000}
     * - For SPECIFIC_USERS: {"userIds": [uuid1, uuid2, ...]}
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();
}

