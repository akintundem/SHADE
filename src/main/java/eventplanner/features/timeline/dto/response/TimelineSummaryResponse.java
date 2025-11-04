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
 * Timeline summary response with statistics and overview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Timeline summary with statistics and overview")
public class TimelineSummaryResponse {
    
    @Schema(description = "Event ID")
    private UUID eventId;
    
    @Schema(description = "Overall progress percentage", example = "45")
    private Integer overallProgress;
    
    @Schema(description = "Status breakdown counts")
    private StatusBreakdown statusBreakdown;
    
    @Schema(description = "Upcoming tasks (next 7 days)")
    private List<TaskSummary> upcomingTasks;
    
    @Schema(description = "Overdue tasks")
    private List<TaskSummary> overdueTasks;
    
    @Schema(description = "Tasks by assignee")
    private Map<UUID, AssigneeTaskSummary> tasksByAssignee;
    
    @Schema(description = "Timeline span")
    private TimelineSpan timelineSpan;
    
    @Schema(description = "Recent activity")
    private List<RecentActivity> recentActivity;
    
    /**
     * Status breakdown
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Status breakdown")
    public static class StatusBreakdown {
        
        @Schema(description = "Total tasks", example = "10")
        private Integer total;
        
        @Schema(description = "TO_DO count", example = "3")
        private Integer toDo;
        
        @Schema(description = "ACTIVE count", example = "4")
        private Integer active;
        
        @Schema(description = "COMPLETED count", example = "2")
        private Integer completed;
        
        @Schema(description = "OVERDUE count", example = "1")
        private Integer overdue;
    }
    
    /**
     * Task summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Task summary")
    public static class TaskSummary {
        
        private UUID taskId;
        private String title;
        private String status;
        private String priority;
        private LocalDateTime dueDate;
        private UUID assignedTo;
        private String assignedToName;
    }
    
    /**
     * Assignee task summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Assignee task summary")
    public static class AssigneeTaskSummary {
        
        private UUID assigneeId;
        private String assigneeName;
        private Integer totalTasks;
        private Integer completedTasks;
        private Integer overdueTasks;
    }
    
    /**
     * Timeline span
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Timeline span")
    public static class TimelineSpan {
        
        private LocalDateTime earliestDate;
        private LocalDateTime latestDate;
        private Long totalDays;
    }
    
    /**
     * Recent activity
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Recent activity")
    public static class RecentActivity {
        
        private UUID taskId;
        private String taskTitle;
        private String action;
        private LocalDateTime timestamp;
        private UUID userId;
        private String userName;
    }
}


