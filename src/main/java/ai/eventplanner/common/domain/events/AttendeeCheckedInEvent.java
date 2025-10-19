package ai.eventplanner.common.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Attendee checked in event for RabbitMQ communication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendeeCheckedInEvent {
    private UUID eventId;
    private UUID userId;
    private UUID attendanceId;
    private String qrCode;
    private LocalDateTime checkInTime;
    private String location;
}
