package eventplanner.features.event.dto.request;

import eventplanner.features.event.enums.EmailTemplateType;
import eventplanner.features.event.enums.RecipientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for event reminders
 * Supports bulk sending to collaborators, vendors, guests, or specific persons
 */
@Schema(description = "Event reminder request")
@Getter
@Setter
public class EventReminderRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be <= 200 chars")
    @Schema(description = "Reminder title")
    private String title;

    @Schema(description = "Reminder description")
    private String description;

    @Schema(description = "When to send the reminder. If not provided, defaults to 5 minutes from now")
    private LocalDateTime reminderTime;

    @NotBlank(message = "Channel is required")
    @Size(max = 30, message = "Channel must be <= 30 chars")
    @Schema(description = "Reminder channel (email, sms, push)", example = "email")
    private String channel;

    @Schema(description = "Email template type for EMAIL channel reminders. Options: ANNOUNCEMENT, CANCEL_EVENT. " +
            "Required when channel is 'email'", example = "ANNOUNCEMENT")
    private EmailTemplateType emailTemplateType;

    @Schema(description = "Recipient types for bulk sending. Options: ALL_COLLABORATORS, ALL_VENDORS, ALL_GUESTS, SPECIFIC_PERSON. " +
            "If SPECIFIC_PERSON is used, recipientUserIds or recipientEmails must be provided.")
    private List<RecipientType> recipientTypes;

    @Schema(description = "List of recipient user IDs (required if SPECIFIC_PERSON is in recipientTypes)")
    private List<UUID> recipientUserIds;

    @Schema(description = "List of recipient email addresses (required if SPECIFIC_PERSON is in recipientTypes)")
    private List<String> recipientEmails;

    @Schema(description = "Reminder type (event_start, registration_deadline, custom)")
    @Size(max = 30, message = "Reminder type must be <= 30 chars")
    private String reminderType = "custom";

    @Schema(description = "Whether reminder is active")
    private Boolean isActive = true;

    @Schema(description = "Custom message for the reminder")
    private String customMessage;

    @Schema(description = "Whether to include event details")
    private Boolean includeEventDetails = true;
}
