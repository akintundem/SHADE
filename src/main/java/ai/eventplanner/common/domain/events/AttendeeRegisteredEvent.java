package ai.eventplanner.common.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Attendee registered event for RabbitMQ communication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendeeRegisteredEvent {
    private UUID eventId;
    private UUID userId;
    private UUID attendanceId;
    private String ticketType;
    private Double ticketPrice;
    private String paymentStatus;
    private LocalDateTime registrationDate;
}
