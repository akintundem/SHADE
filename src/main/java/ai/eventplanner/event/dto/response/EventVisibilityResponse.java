package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Response DTO for event visibility information
 */
@Schema(description = "Event visibility information response")
@Getter
@Setter
public class EventVisibilityResponse {

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Whether the event is public")
    private Boolean isPublic;

    @Schema(description = "Whether the event requires approval")
    private Boolean requiresApproval;

    @Schema(description = "Access level")
    private String accessLevel;

    @Schema(description = "Visibility settings updated timestamp")
    private String updatedAt;
}
