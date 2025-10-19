package ai.eventplanner.event.ai.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiEventTypeResponse {
    String id;
    String name;
    String description;
}
