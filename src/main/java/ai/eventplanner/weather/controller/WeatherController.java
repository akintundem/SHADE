package ai.eventplanner.weather.controller;

import ai.eventplanner.weather.service.OpenWeatherClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/weather")
@Tag(name = "Weather")
public class WeatherController {

    private final OpenWeatherClient client;

    public WeatherController(OpenWeatherClient client) {
        this.client = client;
    }

    @GetMapping("/forecast")
    @Operation(summary = "Get forecast for location")
    public reactor.core.publisher.Mono<org.springframework.http.ResponseEntity<String>> forecast(@RequestParam("lat") String lat, @RequestParam("lon") String lon) {
        return client.getForecast(lat, lon).map(ResponseEntity::ok);
    }
}


