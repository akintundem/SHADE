package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.List;

/**
 * Response DTO for event notification settings
 */
@Schema(description = "Event notification settings response")
@Getter
@Setter
public class EventNotificationSettingsResponse {

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Whether email notifications are enabled")
    private Boolean emailNotifications;

    @Schema(description = "Whether SMS notifications are enabled")
    private Boolean smsNotifications;

    @Schema(description = "Whether push notifications are enabled")
    private Boolean pushNotifications;

    @Schema(description = "Whether reminders are enabled")
    private Boolean reminderEnabled;

    @Schema(description = "Default reminder time")
    private String defaultReminderTime;

    @Schema(description = "Available notification channels")
    private List<String> availableChannels;

    @Schema(description = "Available notification templates")
    private List<String> availableTemplates;

    @Schema(description = "Settings updated timestamp")
    private String updatedAt;
}
