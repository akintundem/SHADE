package ai.eventplanner.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for event reminders
 */
@Schema(description = "Event reminder request")
@Getter
@Setter
public class EventReminderRequest {

    @NotBlank(message = "Title is required")
    @Schema(description = "Reminder title")
    private String title;

    @Schema(description = "Reminder description")
    private String description;

    @NotNull(message = "Reminder time is required")
    @Schema(description = "When to send the reminder")
    private LocalDateTime reminderTime;

    @NotBlank(message = "Channel is required")
    @Schema(description = "Reminder channel (email, sms, push)", example = "email")
    private String channel;

    @Schema(description = "List of recipient user IDs")
    private List<UUID> recipientUserIds;

    @Schema(description = "List of recipient email addresses")
    private List<String> recipientEmails;

    @Schema(description = "Reminder type (event_start, registration_deadline, custom)")
    private String reminderType = "custom";

    @Schema(description = "Whether reminder is active")
    private Boolean isActive = true;

    @Schema(description = "Custom message for the reminder")
    private String customMessage;

    @Schema(description = "Whether to include event details")
    private Boolean includeEventDetails = true;
}
