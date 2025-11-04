package eventplanner.features.timeline.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Optimized response for timeline view visualization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Timeline view response optimized for visualization")
public class TimelineViewResponse {
    
    @Schema(description = "Event ID")
    private UUID eventId;
    
    @Schema(description = "View type", example = "daily")
    private String viewType;
    
    @Schema(description = "Start date of the view range")
    private LocalDateTime viewStartDate;
    
    @Schema(description = "End date of the view range")
    private LocalDateTime viewEndDate;
    
    @Schema(description = "Timeline bars for visualization")
    private List<TimelineBar> timelineBars;
    
    @Schema(description = "Status summary counts")
    private StatusSummary statusSummary;
    
    @Schema(description = "Overall progress percentage", example = "45")
    private Integer overallProgress;
    
    @Schema(description = "Timeline metadata")
    private TimelineMetadata metadata;
    
    /**
     * Timeline bar representation for visualization
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Timeline bar data for visualization")
    public static class TimelineBar {
        
        @Schema(description = "Task ID")
        private UUID taskId;
        
        @Schema(description = "Task title")
        private String title;
        
        @Schema(description = "Start date")
        private LocalDateTime startDate;
        
        @Schema(description = "End date")
        private LocalDateTime endDate;
        
        @Schema(description = "Duration in minutes")
        private Integer durationMinutes;
        
        @Schema(description = "Task status")
        private String status;
        
        @Schema(description = "Priority", example = "HIGH")
        private String priority;
        
        @Schema(description = "Category", example = "LOGISTICS")
        private String category;
        
        @Schema(description = "Progress percentage")
        private Integer progressPercentage;
        
        @Schema(description = "Assigned user ID")
        private UUID assignedTo;
        
        @Schema(description = "Assigned user name")
        private String assignedToName;
        
        @Schema(description = "Parent task ID")
        private UUID parentTaskId;
        
        @Schema(description = "Is this a parent task")
        private Boolean isParentTask;
        
        @Schema(description = "Is in preview state")
        private Boolean isPreview;
        
        @Schema(description = "Color code for UI")
        private String colorCode;
        
        @Schema(description = "List of subtasks")
        private List<TimelineBar> subtasks;
    }
    
    /**
     * Status summary counts
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Status summary counts")
    public static class StatusSummary {
        
        @Schema(description = "Total count", example = "3")
        private Integer all;
        
        @Schema(description = "TO_DO count", example = "1")
        private Integer toDo;
        
        @Schema(description = "ACTIVE count", example = "1")
        private Integer active;
        
        @Schema(description = "COMPLETED/DONE count", example = "1")
        private Integer done;
        
        @Schema(description = "OVERDUE count", example = "0")
        private Integer overdue;
    }
    
    /**
     * Timeline metadata
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Timeline metadata")
    public static class TimelineMetadata {
        
        @Schema(description = "Earliest task date")
        private LocalDateTime earliestDate;
        
        @Schema(description = "Latest task date")
        private LocalDateTime latestDate;
        
        @Schema(description = "Tasks by assignee")
        private Map<UUID, Integer> tasksByAssignee;
        
        @Schema(description = "Tasks by category")
        private Map<String, Integer> tasksByCategory;
    }
}


