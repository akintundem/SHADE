package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for updating event capacity
 */
@Schema(description = "Request to update event capacity")
@Getter
@Setter
public class EventCapacityUpdateRequest {

    @NotNull(message = "Capacity is required")
    @Min(value = 0, message = "Capacity must be non-negative")
    @Schema(description = "New event capacity", example = "200", required = true)
    private Integer capacity;
}
