package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for event reminders
 */
@Schema(description = "Event reminder response")
@Getter
@Setter
public class EventReminderResponse {

    @Schema(description = "Reminder ID")
    private UUID reminderId;

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Reminder title")
    private String title;

    @Schema(description = "Reminder description")
    private String description;

    @Schema(description = "Reminder time")
    private LocalDateTime reminderTime;

    @Schema(description = "Reminder channel")
    private String channel;

    @Schema(description = "Reminder type")
    private String reminderType;

    @Schema(description = "Whether reminder is active")
    private Boolean isActive;

    @Schema(description = "Custom message")
    private String customMessage;

    @Schema(description = "Number of recipients")
    private Integer recipientCount;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Whether reminder was sent")
    private Boolean wasSent;

    @Schema(description = "Actual send time")
    private LocalDateTime sentAt;
}
