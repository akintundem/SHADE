package ai.eventplanner.event.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class AiEventTypeCapabilitiesResponse {
    String eventType;
    String eventTypeName;
    String description;
    List<String> keyFeatures;
    List<String> budgetCategories;
    List<String> recommendedVendors;
    List<String> technologyNeeds;
    List<Map<String, Object>> timelineMonths;
}
