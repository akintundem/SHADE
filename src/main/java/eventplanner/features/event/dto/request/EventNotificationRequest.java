package eventplanner.features.event.dto.request;

import eventplanner.features.event.enums.EventNotificationChannel;
import eventplanner.features.event.enums.EventNotificationPriority;
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
 * Request DTO for event notifications
 * Supports bulk sending to collaborators, vendors, guests, or specific persons
 */
@Schema(description = "Event notification request")
@Getter
@Setter
public class EventNotificationRequest {

    @NotNull(message = "Channel is required")
    @Schema(description = "Notification channel", example = "EMAIL")
    private EventNotificationChannel channel;

    @NotBlank(message = "Subject is required")
    @Schema(description = "Notification subject")
    @Size(max = 200, message = "Subject must be <= 200 chars")
    private String subject;

    @NotBlank(message = "Content is required")
    @Schema(description = "Notification content")
    private String content;

    @Schema(description = "Recipient types for bulk sending. Options: ALL_COLLABORATORS, ALL_VENDORS, ALL_GUESTS, SPECIFIC_PERSON. " +
            "If SPECIFIC_PERSON is used, recipientUserIds or recipientEmails must be provided.")
    private List<RecipientType> recipientTypes;

    @Schema(description = "List of recipient user IDs (required if SPECIFIC_PERSON is in recipientTypes)")
    private List<UUID> recipientUserIds;

    @Schema(description = "List of recipient email addresses (required if SPECIFIC_PERSON is in recipientTypes)")
    private List<String> recipientEmails;

    @Schema(description = "Scheduled send time (if null, send immediately)")
    private LocalDateTime scheduledAt;

    @Schema(description = "Whether to include event details")
    private Boolean includeEventDetails = true;

    @Schema(description = "Notification priority", example = "NORMAL")
    private EventNotificationPriority priority = EventNotificationPriority.NORMAL;

    @Schema(description = "Custom template ID")
    private String templateId;
}
