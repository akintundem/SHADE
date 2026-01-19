package eventplanner.features.attendee.dto.response;

import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RsvpStatusResponse {
    private UUID eventId;
    private UUID attendeeId;
    private AttendeeStatus status;
    private LocalDateTime updatedAt;

    public static RsvpStatusResponse from(Attendee attendee) {
        RsvpStatusResponse res = new RsvpStatusResponse();
        if (attendee != null) {
            res.setAttendeeId(attendee.getId());
            res.setEventId(attendee.getEvent() != null ? attendee.getEvent().getId() : null);
            res.setStatus(attendee.getRsvpStatus());
            res.setUpdatedAt(attendee.getUpdatedAt());
        }
        return res;
    }
}
