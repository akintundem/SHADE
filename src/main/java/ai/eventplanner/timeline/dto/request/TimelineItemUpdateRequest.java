package ai.eventplanner.timeline.dto.request;

import ai.eventplanner.timeline.entity.TimelineItem;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class TimelineItemUpdateRequest {
    private String title;
    private String description;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private TimelineItem.ItemType itemType;
    private String priority;
    private String location;
    private UUID assignedTo;
    private List<UUID> dependencies;
    private Integer setupTimeMinutes;
    private Integer teardownTimeMinutes;
    private String resourcesRequired;
    private String notes;
}
