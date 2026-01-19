package eventplanner.features.attendee.dto.request;

import eventplanner.features.attendee.enums.AttendeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Update RSVP status for an event")
public class UpdateRsvpStatusRequest {

    @NotNull(message = "RSVP status is required")
    @Schema(description = "RSVP status", example = "CONFIRMED")
    private AttendeeStatus status;
}
