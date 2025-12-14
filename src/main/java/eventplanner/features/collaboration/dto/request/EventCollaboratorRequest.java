package eventplanner.features.collaboration.dto.request;

import eventplanner.common.domain.enums.EventUserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for adding/updating event collaborators.
 * Client supplies userId (from directory search) and role/permissions.
 */
@Schema(description = "Event collaborator request")
@Getter
@Setter
public class EventCollaboratorRequest {

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID of the collaborator")
    private UUID userId;

    @NotNull(message = "Role is required")
    @Schema(description = "Role of the collaborator")
    private EventUserType role;

    @Schema(description = "Custom permissions for the collaborator")
    private List<String> permissions;

    @Schema(description = "Notes about the collaborator")
    private String notes;

    @Schema(description = "Whether to send invitation email (optional, currently not used)")
    private Boolean sendInvitation = false;
}





