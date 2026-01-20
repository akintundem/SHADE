package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for cloning an existing event.
 */
@Schema(description = "Request to clone an existing event")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloneEventRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Name for the cloned event. If not provided, will use original name with ' (Copy)' suffix")
    private String name;

    @Schema(description = "Start date and time for the cloned event. If not provided, will use original start date/time")
    private LocalDateTime startDateTime;

    @Schema(description = "End date and time for the cloned event. If not provided, will use original end date/time")
    private LocalDateTime endDateTime;

    @Schema(description = "Whether to clone ticket types from the original event", example = "false")
    private Boolean cloneTicketTypes = false;

    @Schema(description = "Whether to clone venue information from the original event", example = "true")
    private Boolean cloneVenue = true;

    @Schema(description = "Whether to clone media/assets from the original event", example = "false")
    private Boolean cloneMedia = false;

    @Schema(description = "Status for the cloned event. If not provided, will default to DRAFT", example = "DRAFT")
    private eventplanner.features.event.enums.EventStatus eventStatus;
}
