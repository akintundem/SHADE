package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Response DTO for event capacity information
 */
@Schema(description = "Event capacity information response")
@Getter
@Setter
public class EventCapacityResponse {

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Total capacity of the event")
    private Integer capacity;

    @Schema(description = "Current number of attendees")
    private Integer currentAttendeeCount;

    @Schema(description = "Number of available spots")
    private Integer availableSpots;

    @Schema(description = "Capacity utilization percentage")
    private Double utilizationPercentage;

    @Schema(description = "Whether registration is open")
    private Boolean isRegistrationOpen;
}
