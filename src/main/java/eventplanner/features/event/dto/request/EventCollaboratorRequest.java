package eventplanner.features.event.dto.request;

import eventplanner.common.domain.enums.EventUserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for adding event collaborators
 */
@Schema(description = "Event collaborator request")
@Getter
@Setter
public class EventCollaboratorRequest {

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID of the collaborator")
    private UUID userId;

    @Email(message = "Valid email is required")
    @Schema(description = "Email address of the collaborator")
    private String email;

    @NotNull(message = "Role is required")
    @Schema(description = "Role of the collaborator")
    private EventUserType role;

    @Schema(description = "Custom permissions for the collaborator")
    private List<String> permissions;

    @Schema(description = "Notes about the collaborator")
    private String notes;

    @Schema(description = "Whether to send invitation email")
    private Boolean sendInvitation = true;

    @Schema(description = "Custom invitation message")
    private String invitationMessage;
}
