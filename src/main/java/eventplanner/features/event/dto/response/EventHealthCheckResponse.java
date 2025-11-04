package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for event health check
 */
@Schema(description = "Event health check response")
@Getter
@Setter
public class EventHealthCheckResponse {

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Overall health status")
    private String healthStatus;

    @Schema(description = "Health score (0-100)")
    private Integer healthScore;

    @Schema(description = "List of health issues")
    private List<String> issues;

    @Schema(description = "List of health recommendations")
    private List<String> recommendations;

    @Schema(description = "Health check details by component")
    private Map<String, Object> componentHealth;

    @Schema(description = "Health check timestamp")
    private String checkedAt;
}
