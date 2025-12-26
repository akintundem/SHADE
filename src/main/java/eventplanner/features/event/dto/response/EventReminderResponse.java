package eventplanner.features.event.dto.response;

import eventplanner.features.event.entity.EventReminder;
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

    /**
     * Create an EventReminderResponse from an EventReminder entity.
     */
    public static EventReminderResponse from(EventReminder r) {
        EventReminderResponse resp = new EventReminderResponse();
        resp.setReminderId(r.getId());
        resp.setEventId(r.getEvent() != null ? r.getEvent().getId() : null);
        resp.setTitle(r.getTitle());
        resp.setDescription(r.getDescription());
        resp.setReminderTime(r.getReminderTime());
        resp.setChannel(r.getChannel());
        resp.setReminderType(r.getReminderType());
        resp.setIsActive(r.getIsActive());
        resp.setCustomMessage(r.getCustomMessage());
        resp.setRecipientCount(countRecipients(r));
        resp.setCreatedAt(r.getCreatedAt());
        resp.setUpdatedAt(r.getUpdatedAt());
        resp.setWasSent(r.getWasSent());
        return resp;
    }

    private static int countRecipients(EventReminder r) {
        int users = r.getRecipientUserIdsCsv() == null || r.getRecipientUserIdsCsv().isBlank() 
                ? 0 : r.getRecipientUserIdsCsv().split(",").length;
        int emails = r.getRecipientEmailsCsv() == null || r.getRecipientEmailsCsv().isBlank() 
                ? 0 : r.getRecipientEmailsCsv().split(",").length;
        return users + emails;
    }
}
