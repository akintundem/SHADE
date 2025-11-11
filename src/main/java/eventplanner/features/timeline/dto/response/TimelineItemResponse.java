package eventplanner.features.timeline.dto.response;

import eventplanner.common.domain.enums.TimelineStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TimelineItemResponse {
    
    private UUID id;
    private UUID eventId;
    private String title;
    private String description;
    private LocalDateTime scheduledAt;
    private LocalDateTime startDate;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private UUID assignedTo;
    private List<UUID> dependencies;
    private TimelineStatus status;
    private String category;
    private Integer taskOrder;
}
