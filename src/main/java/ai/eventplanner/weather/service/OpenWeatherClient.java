package ai.eventplanner.weather.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OpenWeatherClient {
    private final WebClient webClient;
    private final String apiKey;

    public OpenWeatherClient(@Value("${openweather.baseUrl:https://api.openweathermap.org/data/2.5}") String baseUrl,
                             @Value("${OPENWEATHER_API_KEY:${openweather.api.key:}}") String apiKey) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public Mono<String> getForecast(String lat, String lon) {
        if (apiKey == null || apiKey.isEmpty()) {
            return Mono.just("{\"error\": \"OpenWeather API key not configured\", \"message\": \"Please set OPENWEATHER_API_KEY environment variable\"}");
        }
        return webClient.get()
                .uri(uri -> uri.path("/forecast")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("{\"error\": \"Failed to fetch weather data\", \"message\": \"Unable to connect to OpenWeather API\"}");
    }
}


