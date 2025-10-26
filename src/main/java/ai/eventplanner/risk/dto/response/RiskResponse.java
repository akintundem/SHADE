package ai.eventplanner.risk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for risk assessment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Risk assessment response")
public class RiskResponse {

    @Schema(description = "Risk ID", example = "risk-123")
    private String riskId;

    @Schema(description = "Risk type", example = "weather")
    private String riskType;

    @Schema(description = "Risk level", example = "medium")
    private String riskLevel;

    @Schema(description = "Risk title", example = "Potential rain on event day")
    private String title;

    @Schema(description = "Risk description", example = "Weather forecast shows 60% chance of rain")
    private String description;

    @Schema(description = "Risk probability (0.0 to 1.0)", example = "0.6")
    private Double probability;

    @Schema(description = "Risk impact level", example = "high")
    private String impact;

    @Schema(description = "Mitigation recommendations")
    private String mitigation;

    @Schema(description = "Additional risk metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Risk assessment timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime assessedAt = LocalDateTime.now();
}
