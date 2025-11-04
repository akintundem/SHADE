package eventplanner.features.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for weather forecast data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherForecastResponse {

    private boolean success;
    private String error;
    private CurrentWeather current;
    private List<DailyForecast> forecast;
    private Map<String, Object> location;

    public WeatherForecastResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    /**
     * Current weather information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentWeather {
        private Double temperature;
        private Integer humidity;
        private Double precipitation;
        private Integer weatherCode;
        private String condition;
        private Double windSpeed;
        private Integer windDirection;
    }

    /**
     * Daily forecast information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecast {
        private String date;
        private Double high;
        private Double low;
        private Double precipitationSum;
        private Integer precipitationProbability;
        private Integer weatherCode;
        private String condition;
        private Double windSpeed;
    }
}

