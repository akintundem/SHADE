package eventplanner.features.event.dto.request;

import eventplanner.features.event.enums.EventUserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "Create an event collaborator invite (by userId and/or email)")
public class CreateCollaboratorInviteRequest {

    @Schema(description = "Invitee userId (when inviting an existing user)")
    private UUID inviteeUserId;

    @Email(message = "Valid email is required")
    @Schema(description = "Invitee email (when inviting by email, including non-registered users)")
    private String inviteeEmail;

    @NotNull(message = "Role is required")
    @Schema(description = "Role being granted on acceptance")
    private EventUserType role;

    @Schema(description = "Optional invitation message")
    private String message;

    @Schema(description = "Send email (only meaningful if inviteeEmail is provided)")
    private Boolean sendEmail = true;

    @Schema(description = "Send push notification (only meaningful if inviteeUserId can be resolved)")
    private Boolean sendPush = true;
}

