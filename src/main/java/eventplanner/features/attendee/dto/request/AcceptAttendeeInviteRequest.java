package eventplanner.features.attendee.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;

@Getter
@Setter
@Schema(description = "Accept or decline an attendee invite using token (POST body only; token must not appear in URL)")
public class AcceptAttendeeInviteRequest {
    @NotBlank(message = "Token is required")
    @Schema(description = "Invite acceptance token from email (never send via query string)")
    private String token;

    @NotNull(message = "Status is required")
    @Schema(description = "ACCEPTED or DECLINED", allowableValues = { "ACCEPTED", "DECLINED" })
    private AttendeeInviteStatus status;
}
