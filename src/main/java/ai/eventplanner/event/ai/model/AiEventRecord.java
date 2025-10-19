package ai.eventplanner.event.ai.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AiEventRecord {
    private UUID id;
    private UUID organizerId;
    private String name;
    private String type;
    private OffsetDateTime date;
    private String location;
    private Integer guestCount;
    private BigDecimal budget;
    private String description;
    private List<String> preferences;
    private String notes;
    private String status;
    private boolean aiGenerated;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
