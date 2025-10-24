package ai.eventplanner.weather.controller;

import ai.eventplanner.weather.dto.EventViabilityResponse;
import ai.eventplanner.weather.dto.WeatherForecastResponse;
import ai.eventplanner.weather.service.OpenMeteoClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/weather")
@Tag(name = "Weather", description = "Weather forecast and event planning services using Open-Meteo API")
public class WeatherController {

    private final OpenMeteoClient openMeteoClient;

    public WeatherController(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    @GetMapping("/forecast")
    @Operation(summary = "Get weather forecast for coordinates", 
               description = "Get current weather and 7-day forecast for given latitude and longitude")
    public Mono<ResponseEntity<WeatherForecastResponse>> getForecast(
            @Parameter(description = "Latitude") @RequestParam("lat") String lat,
            @Parameter(description = "Longitude") @RequestParam("lon") String lon) {
        
        return openMeteoClient.getWeatherForecast(lat, lon)
                .map(weatherData -> {
                    WeatherForecastResponse response = new WeatherForecastResponse();
                    response.setSuccess((Boolean) weatherData.getOrDefault("success", false));
                    if (!response.isSuccess()) {
                        response.setError((String) weatherData.get("error"));
                    } else {
                        // Convert Map to CurrentWeather DTO
                        @SuppressWarnings("unchecked")
                        Map<String, Object> currentData = (Map<String, Object>) weatherData.get("current");
                        if (currentData != null) {
                            WeatherForecastResponse.CurrentWeather current = new WeatherForecastResponse.CurrentWeather();
                            current.setTemperature((Double) currentData.get("temperature"));
                            current.setHumidity((Integer) currentData.get("humidity"));
                            current.setPrecipitation((Double) currentData.get("precipitation"));
                            current.setWeatherCode((Integer) currentData.get("weatherCode"));
                            current.setCondition((String) currentData.get("condition"));
                            current.setWindSpeed((Double) currentData.get("windSpeed"));
                            current.setWindDirection((Integer) currentData.get("windDirection"));
                            response.setCurrent(current);
                        }
                        
                        // Convert List of Maps to List of DailyForecast DTOs
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> forecastData = (List<Map<String, Object>>) weatherData.get("forecast");
                        if (forecastData != null) {
                            List<WeatherForecastResponse.DailyForecast> forecast = new ArrayList<>();
                            for (Map<String, Object> dayData : forecastData) {
                                WeatherForecastResponse.DailyForecast day = new WeatherForecastResponse.DailyForecast();
                                day.setDate((String) dayData.get("date"));
                                day.setHigh((Double) dayData.get("high"));
                                day.setLow((Double) dayData.get("low"));
                                day.setPrecipitationSum((Double) dayData.get("precipitationSum"));
                                day.setPrecipitationProbability((Integer) dayData.get("precipitationProbability"));
                                day.setWeatherCode((Integer) dayData.get("weatherCode"));
                                day.setCondition((String) dayData.get("condition"));
                                day.setWindSpeed((Double) dayData.get("windSpeed"));
                                forecast.add(day);
                            }
                            response.setForecast(forecast);
                        }
                    }
                    return ResponseEntity.ok(response);
                });
    }

    @GetMapping("/forecast/location")
    @Operation(summary = "Get weather forecast for location name", 
               description = "Get current weather and forecast for a location by name (e.g., 'New York, NY')")
    public Mono<ResponseEntity<WeatherForecastResponse>> getForecastByLocation(
            @Parameter(description = "Location name") @RequestParam("location") String location) {
        
        return openMeteoClient.geocodeLocation(location)
                .flatMap(geocodeResult -> {
                    if (!(Boolean) geocodeResult.getOrDefault("success", false)) {
                        WeatherForecastResponse errorResponse = new WeatherForecastResponse(false, "Location not found");
                        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> locationData = (Map<String, Object>) geocodeResult.get("location");
                    String lat = locationData.get("latitude").toString();
                    String lon = locationData.get("longitude").toString();
                    
                    return openMeteoClient.getWeatherForecast(lat, lon)
                            .map(weatherData -> {
                                WeatherForecastResponse response = new WeatherForecastResponse();
                                response.setSuccess((Boolean) weatherData.getOrDefault("success", false));
                                if (!response.isSuccess()) {
                                    response.setError((String) weatherData.get("error"));
                                } else {
                                    // Convert Map to CurrentWeather DTO
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> currentData = (Map<String, Object>) weatherData.get("current");
                                    if (currentData != null) {
                                        WeatherForecastResponse.CurrentWeather current = new WeatherForecastResponse.CurrentWeather();
                                        current.setTemperature((Double) currentData.get("temperature"));
                                        current.setHumidity((Integer) currentData.get("humidity"));
                                        current.setPrecipitation((Double) currentData.get("precipitation"));
                                        current.setWeatherCode((Integer) currentData.get("weatherCode"));
                                        current.setCondition((String) currentData.get("condition"));
                                        current.setWindSpeed((Double) currentData.get("windSpeed"));
                                        current.setWindDirection((Integer) currentData.get("windDirection"));
                                        response.setCurrent(current);
                                    }
                                    
                                    // Convert List of Maps to List of DailyForecast DTOs
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> forecastData = (List<Map<String, Object>>) weatherData.get("forecast");
                                    if (forecastData != null) {
                                        List<WeatherForecastResponse.DailyForecast> forecast = new ArrayList<>();
                                        for (Map<String, Object> dayData : forecastData) {
                                            WeatherForecastResponse.DailyForecast day = new WeatherForecastResponse.DailyForecast();
                                            day.setDate((String) dayData.get("date"));
                                            day.setHigh((Double) dayData.get("high"));
                                            day.setLow((Double) dayData.get("low"));
                                            day.setPrecipitationSum((Double) dayData.get("precipitationSum"));
                                            day.setPrecipitationProbability((Integer) dayData.get("precipitationProbability"));
                                            day.setWeatherCode((Integer) dayData.get("weatherCode"));
                                            day.setCondition((String) dayData.get("condition"));
                                            day.setWindSpeed((Double) dayData.get("windSpeed"));
                                            forecast.add(day);
                                        }
                                        response.setForecast(forecast);
                                    }
                                    response.setLocation(locationData);
                                }
                                return ResponseEntity.ok(response);
                            });
                });
    }

    @GetMapping("/event-viability")
    @Operation(summary = "Check outdoor event viability", 
               description = "Analyze weather conditions for outdoor event planning")
    public Mono<ResponseEntity<EventViabilityResponse>> checkEventViability(
            @Parameter(description = "Latitude") @RequestParam("lat") String lat,
            @Parameter(description = "Longitude") @RequestParam("lon") String lon,
            @Parameter(description = "Event date (YYYY-MM-DD)") @RequestParam("eventDate") String eventDate) {
        
        return openMeteoClient.checkOutdoorEventViability(lat, lon, eventDate)
                .map(viabilityData -> {
                    EventViabilityResponse response = new EventViabilityResponse();
                    response.setSuccess((Boolean) viabilityData.getOrDefault("success", false));
                    if (!response.isSuccess()) {
                        response.setError((String) viabilityData.get("error"));
                    } else {
                        response.setViable((Boolean) viabilityData.get("isViable"));
                        response.setViabilityScore((Integer) viabilityData.get("viabilityScore"));
                        @SuppressWarnings("unchecked")
                        Map<String, Object> weatherForecast = (Map<String, Object>) viabilityData.get("weatherForecast");
                        response.setWeatherForecast(weatherForecast);
                        @SuppressWarnings("unchecked")
                        java.util.List<String> concerns = (java.util.List<String>) viabilityData.get("concerns");
                        response.setConcerns(concerns);
                        @SuppressWarnings("unchecked")
                        java.util.List<String> recommendations = (java.util.List<String>) viabilityData.get("recommendations");
                        response.setRecommendations(recommendations);
                    }
                    return ResponseEntity.ok(response);
                });
    }

    @GetMapping("/geocode")
    @Operation(summary = "Geocode location name to coordinates", 
               description = "Convert location name to latitude and longitude coordinates")
    public Mono<ResponseEntity<Map<String, Object>>> geocodeLocation(
            @Parameter(description = "Location name") @RequestParam("location") String location) {
        
        return openMeteoClient.geocodeLocation(location)
                .map(ResponseEntity::ok);
    }
}


