package eventplanner.features.event.dto.response;

import eventplanner.features.event.enums.EventNotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

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

    @Schema(description = "Default reminder offset in minutes")
    private Integer defaultReminderMinutes;

    @Schema(description = "Available notification channels")
    private List<EventNotificationChannel> availableChannels;

    @Schema(description = "Channels currently enabled based on settings")
    private List<EventNotificationChannel> enabledChannels;

    @Schema(description = "Available notification templates")
    private List<String> availableTemplates;

    @Schema(description = "Settings updated timestamp ISO-8601")
    private String updatedAt;
}
