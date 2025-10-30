package ai.eventplanner.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for event notifications
 */
@Schema(description = "Event notification request")
@Getter
@Setter
public class EventNotificationRequest {

    @NotBlank(message = "Channel is required")
    @Schema(description = "Notification channel (email, sms, push)", example = "email")
    @Size(max = 30, message = "Channel must be <= 30 chars")
    private String channel;

    @NotBlank(message = "Subject is required")
    @Schema(description = "Notification subject")
    @Size(max = 200, message = "Subject must be <= 200 chars")
    private String subject;

    @NotBlank(message = "Content is required")
    @Schema(description = "Notification content")
    private String content;

    @Schema(description = "List of recipient user IDs")
    private List<UUID> recipientUserIds;

    @Schema(description = "List of recipient email addresses")
    private List<String> recipientEmails;

    @Schema(description = "Scheduled send time (if null, send immediately)")
    private LocalDateTime scheduledAt;

    @Schema(description = "Whether to include event details")
    private Boolean includeEventDetails = true;

    @Schema(description = "Whether to include QR code")
    private Boolean includeQRCode = false;

    @Schema(description = "Notification priority (low, normal, high)")
    private String priority = "normal";

    @Schema(description = "Custom template ID")
    private String templateId;
}
