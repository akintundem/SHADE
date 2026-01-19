package eventplanner.features.attendee.dto.request;

import eventplanner.features.attendee.enums.AttendeeInviteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "List attendee invites for an event")
public class ListAttendeeInvitesRequest {

    @Schema(description = "Optional status filter")
    private AttendeeInviteStatus status;

    @Schema(description = "Page number (0-based)")
    private Integer page = 0;

    @Schema(description = "Page size (max 100)")
    private Integer size = 20;
}
