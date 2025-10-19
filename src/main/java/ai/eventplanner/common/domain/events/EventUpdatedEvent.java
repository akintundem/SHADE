package ai.eventplanner.common.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event updated event for RabbitMQ communication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventUpdatedEvent {
    private UUID eventId;
    private String eventName;
    private String eventType;
    private String eventStatus;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Integer capacity;
    private Integer currentAttendeeCount;
    private LocalDateTime updatedAt;
}
