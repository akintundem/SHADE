package ai.eventplanner.event.dto.request;

import ai.eventplanner.common.domain.enums.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for updating event status
 */
@Schema(description = "Request to update event status")
@Getter
@Setter
public class EventStatusUpdateRequest {

    @NotNull(message = "Event status is required")
    @Schema(description = "New event status", example = "PUBLISHED", required = true)
    private EventStatus eventStatus;
}
