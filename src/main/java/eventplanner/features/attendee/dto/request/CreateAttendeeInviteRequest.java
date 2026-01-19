package eventplanner.features.attendee.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "Create an attendee invite (by userId and/or email)")
public class CreateAttendeeInviteRequest {

    @Schema(description = "Invitee userId (when inviting an existing user)")
    private UUID inviteeUserId;

    @Email(message = "Valid email is required")
    @Schema(description = "Invitee email (when inviting by email, including non-registered users)")
    private String inviteeEmail;

    @Schema(description = "Optional invitation message")
    private String message;

    @Schema(description = "Send email (only meaningful if inviteeEmail is provided)")
    private Boolean sendEmail = true;

    @Schema(description = "Send push notification (only meaningful if inviteeUserId can be resolved)")
    private Boolean sendPush = true;
}
