package ai.eventplanner.event.dto.request;

import ai.eventplanner.event.enums.EventNotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Schema(description = "Payload for updating event notification settings")
public class EventNotificationSettingsRequest {

    @NotEmpty
    @Schema(description = "Notification channels that should be enabled", example = "[\"EMAIL\", \"PUSH\"]")
    private Set<EventNotificationChannel> enabledChannels;

    @NotNull
    @Schema(description = "Enable reminders", defaultValue = "true")
    private Boolean reminderEnabled;

    @NotNull
    @Schema(description = "Default reminder offset in minutes", example = "1440")
    private Integer defaultReminderMinutes;
}
