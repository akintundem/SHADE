package eventplanner.features.attendee.dto.request;

import eventplanner.features.attendee.enums.AttendeeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkRsvpUpdateItem {

    @NotNull(message = "Attendee ID is required")
    private UUID attendeeId;

    @NotNull(message = "RSVP status is required")
    private AttendeeStatus status;
}
