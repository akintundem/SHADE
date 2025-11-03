package ai.eventplanner.timeline.dto.response;

import ai.eventplanner.common.domain.enums.TimelineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Comprehensive response DTO for a task with all details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Task response with full details including subtasks")
public class TaskResponse {
    
    @Schema(description = "Task ID")
    private UUID id;
    
    @Schema(description = "Event ID")
    private UUID eventId;
    
    @Schema(description = "Task title", example = "Venue Setup")
    private String title;
    
    @Schema(description = "Task description")
    private String description;
    
    @Schema(description = "Task start date")
    private LocalDateTime startDate;
    
    @Schema(description = "Task due date")
    private LocalDateTime dueDate;
    
    @Schema(description = "Scheduled at (legacy field)")
    private LocalDateTime scheduledAt;
    
    @Schema(description = "End time")
    private LocalDateTime endTime;
    
    @Schema(description = "Duration in minutes")
    private Integer durationMinutes;
    
    @Schema(description = "Priority level", example = "HIGH")
    private String priority;
    
    @Schema(description = "Task category", example = "LOGISTICS")
    private String category;
    
    @Schema(description = "Task status")
    private TimelineStatus status;
    
    @Schema(description = "Progress percentage (0-100)", example = "50")
    private Integer progressPercentage;
    
    @Schema(description = "Number of completed subtasks")
    private Integer completedSubtasksCount;
    
    @Schema(description = "Total number of subtasks")
    private Integer totalSubtasksCount;
    
    @Schema(description = "User ID assigned to this task")
    private UUID assignedTo;
    
    @Schema(description = "Assigned user name")
    private String assignedToName;
    
    @Schema(description = "Parent task ID if this is a subtask")
    private UUID parentTaskId;
    
    @Schema(description = "Order of this task among siblings")
    private Integer taskOrder;
    
    @Schema(description = "Whether this is a parent task")
    private Boolean isParentTask;
    
    @Schema(description = "Whether task is in preview/draft state")
    private Boolean isPreview;
    
    @Schema(description = "List of dependency task IDs")
    private List<UUID> dependencies;
    
    @Schema(description = "List of subtasks (if this is a parent task)")
    private List<TaskResponse> subtasks;
    
    @Schema(description = "Payment date")
    private LocalDateTime paymentDate;
    
    @Schema(description = "Proof image URL (single image)")
    private String proofImageUrl;
    
    @Schema(description = "Proof image URLs (multiple images)")
    private List<String> proofImageUrls;
    
    @Schema(description = "Created at timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Updated at timestamp")
    private LocalDateTime updatedAt;
}

