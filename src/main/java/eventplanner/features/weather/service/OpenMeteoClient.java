package eventplanner.features.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenMeteoClient {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public OpenMeteoClient() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.open-meteo.com/v1")
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    private WebClient getGeocodingClient() {
        return WebClient.builder()
                .baseUrl("https://geocoding-api.open-meteo.com/v1")
                .build();
    }
    
    /**
     * Get current weather and forecast for a location
     */
    public Mono<Map<String, Object>> getWeatherForecast(String lat, String lon) {
        return webClient.get()
                .uri(uri -> uri.path("/forecast")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("current", "temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m")
                        .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,weather_code,wind_speed_10m_max")
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseWeatherResponse)
                .onErrorReturn(createErrorResponse("Failed to fetch weather data"));
    }
    
    /**
     * Get weather forecast for a specific date range
     */
    public Mono<Map<String, Object>> getWeatherForecast(String lat, String lon, String startDate, String endDate) {
        return webClient.get()
                .uri(uri -> uri.path("/forecast")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("start_date", startDate)
                        .queryParam("end_date", endDate)
                        .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,weather_code,wind_speed_10m_max")
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseWeatherResponse)
                .onErrorReturn(createErrorResponse("Failed to fetch weather data"));
    }
    
    /**
     * Geocode a location name to coordinates
     */
    public Mono<Map<String, Object>> geocodeLocation(String location) {
        System.out.println("Geocoding location: " + location);
        return getGeocodingClient().get()
                .uri(uri -> uri.path("/search")
                        .queryParam("name", location)
                        .queryParam("count", "1")
                        .queryParam("language", "en")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> System.out.println("Geocoding response: " + response))
                .map(this::parseGeocodeResponse)
                .doOnError(error -> System.out.println("Geocoding error: " + error.getMessage()))
                .onErrorReturn(createErrorResponse("Failed to geocode location"));
    }
    
    /**
     * Check outdoor event viability for a specific date
     */
    public Mono<Map<String, Object>> checkOutdoorEventViability(String lat, String lon, String eventDate) {
        return getWeatherForecast(lat, lon)
                .map(weatherData -> analyzeEventViability(weatherData, eventDate));
    }
    
    private Map<String, Object> parseWeatherResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            Map<String, Object> result = new HashMap<>();
            
            // Parse current weather
            JsonNode current = jsonNode.get("current");
            if (current != null) {
                Map<String, Object> currentWeather = new HashMap<>();
                currentWeather.put("temperature", current.get("temperature_2m").asDouble());
                currentWeather.put("humidity", current.get("relative_humidity_2m").asInt());
                currentWeather.put("precipitation", current.get("precipitation").asDouble());
                currentWeather.put("weatherCode", current.get("weather_code").asInt());
                currentWeather.put("windSpeed", current.get("wind_speed_10m").asDouble());
                currentWeather.put("windDirection", current.get("wind_direction_10m").asInt());
                currentWeather.put("condition", getWeatherCondition(current.get("weather_code").asInt()));
                result.put("current", currentWeather);
            }
            
            // Parse daily forecast
            JsonNode daily = jsonNode.get("daily");
            if (daily != null) {
                List<Map<String, Object>> forecast = new ArrayList<>();
                JsonNode dates = daily.get("time");
                JsonNode maxTemps = daily.get("temperature_2m_max");
                JsonNode minTemps = daily.get("temperature_2m_min");
                JsonNode precipSums = daily.get("precipitation_sum");
                JsonNode precipProbs = daily.get("precipitation_probability_max");
                JsonNode weatherCodes = daily.get("weather_code");
                JsonNode windSpeeds = daily.get("wind_speed_10m_max");
                
                for (int i = 0; i < dates.size(); i++) {
                    Map<String, Object> dayForecast = new HashMap<>();
                    dayForecast.put("date", dates.get(i).asText());
                    dayForecast.put("high", maxTemps.get(i).asDouble());
                    dayForecast.put("low", minTemps.get(i).asDouble());
                    dayForecast.put("precipitationSum", precipSums.get(i).asDouble());
                    dayForecast.put("precipitationProbability", precipProbs.get(i).asInt());
                    dayForecast.put("weatherCode", weatherCodes.get(i).asInt());
                    dayForecast.put("windSpeed", windSpeeds.get(i).asDouble());
                    dayForecast.put("condition", getWeatherCondition(weatherCodes.get(i).asInt()));
                    forecast.add(dayForecast);
                }
                result.put("forecast", forecast);
            }
            
            result.put("success", true);
            return result;
            
        } catch (Exception e) {
            return createErrorResponse("Failed to parse weather response: " + e.getMessage());
        }
    }
    
    private Map<String, Object> parseGeocodeResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            Map<String, Object> result = new HashMap<>();
            
            JsonNode results = jsonNode.get("results");
            if (results != null && results.size() > 0) {
                JsonNode firstResult = results.get(0);
                Map<String, Object> location = new HashMap<>();
                location.put("latitude", firstResult.get("latitude").asDouble());
                location.put("longitude", firstResult.get("longitude").asDouble());
                location.put("name", firstResult.get("name").asText());
                location.put("country", firstResult.get("country").asText());
                location.put("admin1", firstResult.get("admin1").asText());
                result.put("location", location);
                result.put("success", true);
            } else {
                result.put("success", false);
                result.put("error", "No location found");
            }
            
            return result;
            
        } catch (Exception e) {
            return createErrorResponse("Failed to parse geocode response: " + e.getMessage());
        }
    }
    
    private Map<String, Object> analyzeEventViability(Map<String, Object> weatherData, String eventDate) {
        Map<String, Object> result = new HashMap<>();
        
        if (!(Boolean) weatherData.getOrDefault("success", false)) {
            result.put("success", false);
            result.put("error", "Weather data unavailable");
            return result;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> forecast = (List<Map<String, Object>>) weatherData.get("forecast");
        
        // Find forecast for the event date
        Map<String, Object> eventForecast = null;
        for (Map<String, Object> day : forecast) {
            if (eventDate.equals(day.get("date"))) {
                eventForecast = day;
                break;
            }
        }
        
        if (eventForecast == null) {
            result.put("success", false);
            result.put("error", "No forecast available for event date");
            return result;
        }
        
        // Analyze viability
        boolean isViable = true;
        List<String> concerns = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        double precipitationProb = (Integer) eventForecast.get("precipitationProbability");
        double windSpeed = (Double) eventForecast.get("windSpeed");
        double highTemp = (Double) eventForecast.get("high");
        double lowTemp = (Double) eventForecast.get("low");
        int weatherCode = (Integer) eventForecast.get("weatherCode");
        
        // Check precipitation risk
        if (precipitationProb > 50) {
            isViable = false;
            concerns.add("High chance of precipitation");
            recommendations.add("Consider indoor backup venue");
        } else if (precipitationProb > 30) {
            concerns.add("Moderate precipitation risk");
            recommendations.add("Prepare covered areas and umbrellas");
        }
        
        // Check wind risk
        if (windSpeed > 20) {
            concerns.add("High wind speeds");
            recommendations.add("Secure outdoor decorations and equipment");
        } else if (windSpeed > 15) {
            concerns.add("Moderate wind conditions");
            recommendations.add("Secure lightweight items");
        }
        
        // Check temperature extremes
        if (highTemp > 35 || lowTemp < 5) {
            concerns.add("Extreme temperature conditions");
            recommendations.add("Provide climate control or heating/cooling");
        } else if (highTemp > 30 || lowTemp < 10) {
            concerns.add("Challenging temperature conditions");
            recommendations.add("Provide shade and heating/cooling options");
        }
        
        // Check for severe weather codes
        if (weatherCode >= 95) { // Thunderstorm
            isViable = false;
            concerns.add("Severe weather warning");
            recommendations.add("Postpone or move indoors immediately");
        } else if (weatherCode >= 80) { // Rain showers
            concerns.add("Rain showers expected");
            recommendations.add("Prepare covered areas");
        }
        
        // Calculate viability score
        int viabilityScore = 100;
        if (!isViable) {
            viabilityScore = 20;
        } else if (concerns.size() >= 3) {
            viabilityScore = 40;
        } else if (concerns.size() >= 2) {
            viabilityScore = 60;
        } else if (concerns.size() >= 1) {
            viabilityScore = 80;
        }
        
        result.put("success", true);
        result.put("isViable", isViable);
        result.put("viabilityScore", viabilityScore);
        result.put("weatherForecast", eventForecast);
        result.put("concerns", concerns);
        result.put("recommendations", recommendations);
        
        return result;
    }
    
    private String getWeatherCondition(int weatherCode) {
        return switch (weatherCode) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45 -> "Fog";
            case 48 -> "Depositing rime fog";
            case 51 -> "Light drizzle";
            case 53 -> "Moderate drizzle";
            case 55 -> "Dense drizzle";
            case 61 -> "Slight rain";
            case 63 -> "Moderate rain";
            case 65 -> "Heavy rain";
            case 71 -> "Slight snow";
            case 73 -> "Moderate snow";
            case 75 -> "Heavy snow";
            case 77 -> "Snow grains";
            case 80 -> "Slight rain showers";
            case 81 -> "Moderate rain showers";
            case 82 -> "Violent rain showers";
            case 85 -> "Slight snow showers";
            case 86 -> "Heavy snow showers";
            case 95 -> "Thunderstorm";
            case 96 -> "Thunderstorm with slight hail";
            case 99 -> "Thunderstorm with heavy hail";
            default -> "Unknown";
        };
    }
    
    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", error);
        return result;
    }
}
