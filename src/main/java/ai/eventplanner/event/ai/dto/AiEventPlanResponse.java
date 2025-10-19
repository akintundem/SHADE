package ai.eventplanner.event.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class AiEventPlanResponse {
    UUID eventId;
    String eventName;
    String eventType;
    String date;
    String location;
    Integer guestCount;
    BigDecimal budget;
    List<String> recommendations;
    Map<String, BigDecimal> budgetBreakdown;
    List<Map<String, Object>> timeline;
    List<Map<String, Object>> vendorRecommendations;
    List<String> nextSteps;
    String status;
}
