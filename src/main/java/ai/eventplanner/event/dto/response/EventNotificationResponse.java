package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for event notifications
 */
@Schema(description = "Event notification response")
@Getter
@Setter
public class EventNotificationResponse {

    @Schema(description = "Notification ID")
    private UUID notificationId;

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Notification channel")
    private String channel;

    @Schema(description = "Notification subject")
    private String subject;

    @Schema(description = "Notification content")
    private String content;

    @Schema(description = "Notification status")
    private String status;

    @Schema(description = "Number of recipients")
    private Integer recipientCount;

    @Schema(description = "Scheduled send time")
    private LocalDateTime scheduledAt;

    @Schema(description = "Actual send time")
    private LocalDateTime sentAt;

    @Schema(description = "Notification priority")
    private String priority;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "List of successful recipients")
    private List<String> successfulRecipients;

    @Schema(description = "List of failed recipients")
    private List<String> failedRecipients;

    @Schema(description = "Error message if failed")
    private String errorMessage;
}
