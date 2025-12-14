package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Respond to a collaborator invite (optional client note)")
public class RespondToCollaboratorInviteRequest {
    @Schema(description = "Optional response note (for audit/UI)")
    private String note;
}

