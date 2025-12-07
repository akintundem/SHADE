package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for duplicating an event
 */
@Schema(description = "Request to duplicate an event")
@Getter
@Setter
public class EventDuplicateRequest {

    @NotBlank(message = "New event name is required")
    @Size(max = 255, message = "Event name must not exceed 255 characters")
    @Schema(description = "Name for the duplicated event", example = "Copy of Annual Conference", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newEventName;

    @Schema(description = "Whether to copy attendees", example = "false")
    private Boolean copyAttendees = false;

    @Schema(description = "Whether to copy media", example = "true")
    private Boolean copyMedia = true;

    @Schema(description = "Whether to copy settings", example = "true")
    private Boolean copySettings = true;
}
