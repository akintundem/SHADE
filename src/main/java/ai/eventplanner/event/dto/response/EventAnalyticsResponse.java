package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.Map;

/**
 * Response DTO for event analytics
 */
@Schema(description = "Event analytics response")
@Getter
@Setter
public class EventAnalyticsResponse {

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Total views")
    private Long totalViews;

    @Schema(description = "Unique visitors")
    private Long uniqueVisitors;

    @Schema(description = "Registration rate")
    private Double registrationRate;

    @Schema(description = "Attendance rate")
    private Double attendanceRate;

    @Schema(description = "Engagement metrics")
    private Map<String, Object> engagementMetrics;

    @Schema(description = "Social media metrics")
    private Map<String, Object> socialMetrics;

    @Schema(description = "Geographic distribution")
    private Map<String, Object> geographicDistribution;

    @Schema(description = "Analytics period")
    private String analyticsPeriod;
}
