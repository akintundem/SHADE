package ai.eventplanner.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class WeatherForecastResponse {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("current")
    private CurrentWeather current;
    
    @JsonProperty("forecast")
    private List<DailyForecast> forecast;
    
    @JsonProperty("location")
    private Map<String, Object> location;
    
    public WeatherForecastResponse() {}
    
    public WeatherForecastResponse(boolean success, String error) {
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
    
    public CurrentWeather getCurrent() {
        return current;
    }
    
    public void setCurrent(CurrentWeather current) {
        this.current = current;
    }
    
    public List<DailyForecast> getForecast() {
        return forecast;
    }
    
    public void setForecast(List<DailyForecast> forecast) {
        this.forecast = forecast;
    }
    
    public Map<String, Object> getLocation() {
        return location;
    }
    
    public void setLocation(Map<String, Object> location) {
        this.location = location;
    }
    
    public static class CurrentWeather {
        @JsonProperty("temperature")
        private double temperature;
        
        @JsonProperty("humidity")
        private int humidity;
        
        @JsonProperty("precipitation")
        private double precipitation;
        
        @JsonProperty("weatherCode")
        private int weatherCode;
        
        @JsonProperty("windSpeed")
        private double windSpeed;
        
        @JsonProperty("windDirection")
        private int windDirection;
        
        @JsonProperty("condition")
        private String condition;
        
        // Getters and Setters
        public double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
        
        public int getHumidity() {
            return humidity;
        }
        
        public void setHumidity(int humidity) {
            this.humidity = humidity;
        }
        
        public double getPrecipitation() {
            return precipitation;
        }
        
        public void setPrecipitation(double precipitation) {
            this.precipitation = precipitation;
        }
        
        public int getWeatherCode() {
            return weatherCode;
        }
        
        public void setWeatherCode(int weatherCode) {
            this.weatherCode = weatherCode;
        }
        
        public double getWindSpeed() {
            return windSpeed;
        }
        
        public void setWindSpeed(double windSpeed) {
            this.windSpeed = windSpeed;
        }
        
        public int getWindDirection() {
            return windDirection;
        }
        
        public void setWindDirection(int windDirection) {
            this.windDirection = windDirection;
        }
        
        public String getCondition() {
            return condition;
        }
        
        public void setCondition(String condition) {
            this.condition = condition;
        }
    }
    
    public static class DailyForecast {
        @JsonProperty("date")
        private String date;
        
        @JsonProperty("high")
        private double high;
        
        @JsonProperty("low")
        private double low;
        
        @JsonProperty("precipitationSum")
        private double precipitationSum;
        
        @JsonProperty("precipitationProbability")
        private int precipitationProbability;
        
        @JsonProperty("weatherCode")
        private int weatherCode;
        
        @JsonProperty("windSpeed")
        private double windSpeed;
        
        @JsonProperty("condition")
        private String condition;
        
        // Getters and Setters
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
        
        public double getHigh() {
            return high;
        }
        
        public void setHigh(double high) {
            this.high = high;
        }
        
        public double getLow() {
            return low;
        }
        
        public void setLow(double low) {
            this.low = low;
        }
        
        public double getPrecipitationSum() {
            return precipitationSum;
        }
        
        public void setPrecipitationSum(double precipitationSum) {
            this.precipitationSum = precipitationSum;
        }
        
        public int getPrecipitationProbability() {
            return precipitationProbability;
        }
        
        public void setPrecipitationProbability(int precipitationProbability) {
            this.precipitationProbability = precipitationProbability;
        }
        
        public int getWeatherCode() {
            return weatherCode;
        }
        
        public void setWeatherCode(int weatherCode) {
            this.weatherCode = weatherCode;
        }
        
        public double getWindSpeed() {
            return windSpeed;
        }
        
        public void setWindSpeed(double windSpeed) {
            this.windSpeed = windSpeed;
        }
        
        public String getCondition() {
            return condition;
        }
        
        public void setCondition(String condition) {
            this.condition = condition;
        }
    }
}
