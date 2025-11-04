package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for updating event visibility
 */
@Schema(description = "Request to update event visibility")
@Getter
@Setter
public class EventVisibilityUpdateRequest {

    @NotNull(message = "Public status is required")
    @Schema(description = "Whether the event is public", example = "true", required = true)
    private Boolean isPublic;

    @Schema(description = "Whether the event requires approval", example = "false")
    private Boolean requiresApproval;
}
