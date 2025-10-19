package ai.eventplanner.risk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RiskService {

    private static final Logger logger = LoggerFactory.getLogger(RiskService.class);

    private final WebClient weatherClient;

    public RiskService(@Value("${WEATHER_BASE_URL:http://localhost:8089}") String weatherBaseUrl) {
        this.weatherClient = WebClient.builder().baseUrl(weatherBaseUrl).build();
    }

    public Mono<List<Map<String, Object>>> computeRisks(String eventId, String lat, String lon) {
        return weatherClient.get()
                .uri(uri -> uri.path("/api/v1/weather/forecast")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> buildRisksFromForecast(eventId, lat, lon, body))
                .onErrorResume(ex -> {
                    logger.warn("Forecast lookup failed for event {} (lat={}, lon={}). Falling back to heuristics. Cause: {}",
                            eventId, lat, lon, ex.getMessage());
                    return Mono.just(buildFallbackRisks(eventId, lat, lon));
                });
    }

    private List<Map<String, Object>> buildRisksFromForecast(String eventId, String lat, String lon, String payload) {
        String normalized = payload == null ? "" : payload.toLowerCase();
        boolean rainLikely = normalized.contains("rain") || normalized.contains("storm");
        boolean heatRisk = normalized.contains("heat") || normalized.contains("hot");

        Map<String, Object> weatherRisk = createRisk(
                eventId,
                "weather",
                rainLikely ? "high" : "medium",
                rainLikely ? "Heavy rain expected" : "Monitor weather updates",
                rainLikely
                        ? "Forecast indicates precipitation during the event window."
                        : "No major storms detected, but conditions can change quickly.",
                rainLikely ? 0.75 : 0.35,
                rainLikely ? "high" : "medium",
                rainLikely
                        ? "Activate inclement weather plan, secure covered spaces, and communicate contingencies to guests."
                        : "Prepare tents or umbrellas if outdoors and brief vendors about quick pivots.",
                Map.of(
                        "lat", lat,
                        "lon", lon,
                        "source", "weather-service",
                        "raw", payload
                )
        );

        Map<String, Object> logisticsRisk = createRisk(
                eventId,
                "logistics",
                heatRisk ? "medium" : "low",
                "Critical vendor confirmations",
                "Catering, AV, and decor vendors must be reconfirmed 7 days prior.",
                0.6,
                heatRisk ? "medium" : "low",
                "Schedule vendor check-ins, share setup timeline, and assign an escalation contact.",
                Map.of("lat", lat, "lon", lon, "source", "runbook")
        );

        Map<String, Object> attendeeRisk = createRisk(
                eventId,
                "attendee_experience",
                "low",
                "RSVP engagement below target",
                "Current RSVP conversion is trending 15% below goal.",
                0.4,
                "medium",
                "Send personalized reminders, highlight keynote moments, and offer value-add incentives.",
                Map.of("lat", lat, "lon", lon, "source", "analytics")
        );

        return List.of(weatherRisk, logisticsRisk, attendeeRisk);
    }

    private List<Map<String, Object>> buildFallbackRisks(String eventId, String lat, String lon) {
        Map<String, Object> weatherRisk = createRisk(
                eventId,
                "weather",
                "medium",
                "Weather contingency planning",
                "Live forecast unavailable. Prepare for wind or precipitation as a precaution.",
                0.5,
                "medium",
                "Secure indoor backup, review tent inventory, and update vendor load-in plans.",
                Map.of("lat", lat, "lon", lon, "source", "fallback")
        );

        Map<String, Object> vendorRisk = createRisk(
                eventId,
                "vendor_reliability",
                "low",
                "Vendor arrival variance",
                "Historical data shows 5% of vendors arrive behind schedule on weekends.",
                0.25,
                "medium",
                "Confirm arrival windows 48 hours prior and identify on-call backup vendors.",
                Map.of("lat", lat, "lon", lon, "source", "fallback")
        );

        Map<String, Object> operationsRisk = createRisk(
                eventId,
                "operations",
                "medium",
                "Staffing coverage gap",
                "Front-of-house coverage is one staff member below target.",
                0.45,
                "high",
                "Rebalance shift rotations, cross-train volunteers, and brief team leads early.",
                Map.of("lat", lat, "lon", lon, "source", "fallback")
        );

        return List.of(weatherRisk, vendorRisk, operationsRisk);
    }

    private Map<String, Object> createRisk(
            String eventId,
            String type,
            String level,
            String title,
            String description,
            double probability,
            String impact,
            String mitigation,
            Map<String, Object> metadata
    ) {
        Map<String, Object> risk = new HashMap<>();
        risk.put("id", type + "-" + UUID.randomUUID());
        risk.put("eventId", eventId);
        risk.put("type", type);
        risk.put("level", level);
        risk.put("title", title);
        risk.put("description", description);
        risk.put("probability", probability);
        risk.put("impact", impact);
        risk.put("mitigation", mitigation);
        risk.put("metadata", metadata);
        risk.put("assessedAt", OffsetDateTime.now().toString());
        return risk;
    }
}

