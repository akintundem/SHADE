package eventplanner.features.event.dto.response;

import eventplanner.features.event.entity.EventNotificationSettings;
import eventplanner.features.event.enums.EventNotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for event notification settings
 */
@Schema(description = "Event notification settings response")
@Getter
@Setter
public class EventNotificationSettingsResponse {

    private static final List<EventNotificationChannel> SUPPORTED_CHANNELS = 
            List.of(EventNotificationChannel.EMAIL, EventNotificationChannel.SMS, EventNotificationChannel.PUSH);

    private static final List<String> AVAILABLE_TEMPLATES = 
            List.of("event_reminder", "event_update", "event_cancelled");

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

    /**
     * Create an EventNotificationSettingsResponse from an EventNotificationSettings entity.
     */
    public static EventNotificationSettingsResponse from(EventNotificationSettings settings) {
        EventNotificationSettingsResponse response = new EventNotificationSettingsResponse();
        response.setEventId(settings.getEvent() != null ? settings.getEvent().getId() : null);
        response.setEmailNotifications(settings.getEmailEnabled());
        response.setSmsNotifications(settings.getSmsEnabled());
        response.setPushNotifications(settings.getPushEnabled());
        response.setReminderEnabled(settings.getReminderEnabled());
        response.setDefaultReminderMinutes(settings.getDefaultReminderMinutes());
        response.setAvailableChannels(SUPPORTED_CHANNELS);
        response.setEnabledChannels(buildEnabledChannels(settings));
        if (settings.getUpdatedAt() != null) {
            response.setUpdatedAt(settings.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        response.setAvailableTemplates(AVAILABLE_TEMPLATES);
        return response;
    }

    private static List<EventNotificationChannel> buildEnabledChannels(EventNotificationSettings settings) {
        return SUPPORTED_CHANNELS.stream()
                .filter(channel -> switch (channel) {
                    case EMAIL -> Boolean.TRUE.equals(settings.getEmailEnabled());
                    case SMS -> Boolean.TRUE.equals(settings.getSmsEnabled());
                    case PUSH -> Boolean.TRUE.equals(settings.getPushEnabled());
                })
                .toList();
    }
}
