package ai.eventplanner.event.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class AiEventResponse {
    UUID id;
    String name;
    String type;
    String date;
    String location;
    UUID organizerId;
    Integer guestCount;
    java.math.BigDecimal budget;
    String description;
    List<String> preferences;
    String notes;
    String status;
    boolean aiGenerated;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
