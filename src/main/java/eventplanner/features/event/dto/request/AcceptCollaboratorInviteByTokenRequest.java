package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Accept a collaborator invite using an email token")
public class AcceptCollaboratorInviteByTokenRequest {
    @NotBlank(message = "Token is required")
    @Schema(description = "The invite acceptance token from the email")
    private String token;
}
