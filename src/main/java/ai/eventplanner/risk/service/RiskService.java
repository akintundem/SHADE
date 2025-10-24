package ai.eventplanner.risk.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RiskService {

    public RiskService() {
        // Risk service without weather dependency
    }

    public Mono<List<Map<String, Object>>> computeRisks(String eventId, String lat, String lon) {
        // Basic risk assessment without weather dependency
        return Mono.just(buildBasicRisks(eventId, lat, lon));
    }

    private List<Map<String, Object>> buildBasicRisks(String eventId, String lat, String lon) {
        Map<String, Object> logisticsRisk = createRisk(
                eventId,
                "logistics",
                "medium",
                "Event logistics planning",
                "Standard event logistics considerations apply.",
                0.6,
                "medium",
                "Plan vendor coordination, setup timeline, and backup contingencies.",
                Map.of("lat", lat, "lon", lon, "source", "basic-assessment")
        );

        Map<String, Object> attendeeRisk = createRisk(
                eventId,
                "attendee_experience",
                "low",
                "RSVP engagement monitoring",
                "Monitor RSVP conversion and engagement metrics.",
                0.4,
                "medium",
                "Track RSVP rates and send follow-up communications as needed.",
                Map.of("lat", lat, "lon", lon, "source", "basic-assessment")
        );

        Map<String, Object> operationsRisk = createRisk(
                eventId,
                "operations",
                "low",
                "Staff coordination",
                "Ensure adequate staffing coverage for the event.",
                0.3,
                "medium",
                "Confirm staff schedules and cross-train team members.",
                Map.of("lat", lat, "lon", lon, "source", "basic-assessment")
        );

        return List.of(logisticsRisk, attendeeRisk, operationsRisk);
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

