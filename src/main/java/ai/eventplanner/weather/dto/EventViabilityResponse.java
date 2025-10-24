package ai.eventplanner.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class EventViabilityResponse {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("isViable")
    private boolean isViable;
    
    @JsonProperty("viabilityScore")
    private int viabilityScore;
    
    @JsonProperty("weatherForecast")
    private Map<String, Object> weatherForecast;
    
    @JsonProperty("concerns")
    private List<String> concerns;
    
    @JsonProperty("recommendations")
    private List<String> recommendations;
    
    public EventViabilityResponse() {}
    
    public EventViabilityResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public boolean isViable() {
        return isViable;
    }
    
    public void setViable(boolean viable) {
        isViable = viable;
    }
    
    public int getViabilityScore() {
        return viabilityScore;
    }
    
    public void setViabilityScore(int viabilityScore) {
        this.viabilityScore = viabilityScore;
    }
    
    public Map<String, Object> getWeatherForecast() {
        return weatherForecast;
    }
    
    public void setWeatherForecast(Map<String, Object> weatherForecast) {
        this.weatherForecast = weatherForecast;
    }
    
    public List<String> getConcerns() {
        return concerns;
    }
    
    public void setConcerns(List<String> concerns) {
        this.concerns = concerns;
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
}
