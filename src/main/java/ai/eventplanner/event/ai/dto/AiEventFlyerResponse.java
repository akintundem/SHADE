package ai.eventplanner.event.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AiEventFlyerResponse {
    UUID eventId;
    String status;
    String message;
    String flyerUrl;
    String thumbnailUrl;
    String printUrl;
    String designDescription;
}
