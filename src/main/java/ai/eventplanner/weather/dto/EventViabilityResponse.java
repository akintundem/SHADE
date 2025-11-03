package ai.eventplanner.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for event viability analysis
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventViabilityResponse {

    private boolean success;
    private String error;
    private Boolean viable;
    private Integer viabilityScore;
    private Map<String, Object> weatherForecast;
    private List<String> concerns;
    private List<String> recommendations;

    public EventViabilityResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }
}

