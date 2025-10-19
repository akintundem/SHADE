package ai.eventplanner.attendee.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AttendeeResponse {
    
    private UUID id;
    private UUID eventId;
    private String name;
    private String email;
    private String phone;
    private String rsvpStatus;
    private LocalDateTime checkedInAt;
}
