package ai.eventplanner.timeline.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TimelineItemResponse {
    
    private UUID id;
    private UUID eventId;
    private String title;
    private String description;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private UUID assignedTo;
    private UUID[] dependencies;
    private String status;
}
