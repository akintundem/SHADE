package eventplanner.features.timeline.dto.response;

import eventplanner.features.timeline.enums.TimelineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Full task details including checklist items")
public class TaskDetailResponse {
    
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime dueDate;
    private String priority;
    private String category;
    private TimelineStatus status;
    private Integer progressPercentage;
    private UUID assignedTo;
    private String assignedToName;
    private Integer taskOrder;
    private Integer completedSubtasksCount;
    private Integer totalSubtasksCount;
    private Boolean isDraft;
    private List<ChecklistItemResponse> checklist;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Checklist item response")
    public static class ChecklistItemResponse {
        private UUID id;
        private String title;
        private String description;
        private LocalDateTime dueDate;
        private TimelineStatus status;
        private UUID assignedTo;
        private String assignedToName;
        private Integer taskOrder;
        private Boolean isDraft;
    }
}

