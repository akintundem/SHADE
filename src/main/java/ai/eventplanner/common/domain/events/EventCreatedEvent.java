package ai.eventplanner.common.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event created event for RabbitMQ communication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventCreatedEvent {
    private UUID eventId;
    private String eventName;
    private String eventType;
    private UUID organizerId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Integer capacity;
    private Boolean qrCodeEnabled;
    private LocalDateTime createdAt;
}
