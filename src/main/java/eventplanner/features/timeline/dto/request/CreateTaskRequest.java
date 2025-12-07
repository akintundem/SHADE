package eventplanner.features.timeline.dto.request;

import eventplanner.common.domain.enums.TimelineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a task with optional subtasks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a task with optional subtasks")
public class CreateTaskRequest {
    
    @NotNull(message = "Event ID is required")
    @Schema(description = "Event ID this task belongs to", requiredMode = Schema.RequiredMode.REQUIRED, example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID eventId;
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Task title", requiredMode = Schema.RequiredMode.REQUIRED, example = "Venue Setup")
    private String title;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Task description", example = "Coordinate with venue management")
    private String description;
    
    @Schema(description = "Task start date and time", example = "2024-05-09T08:00:00")
    private LocalDateTime startDate;
    
    @Schema(description = "Task due date", example = "2024-05-19T17:00:00")
    private LocalDateTime dueDate;
    
    @Schema(description = "Duration in minutes", example = "480")
    private Integer durationMinutes;
    
    @Builder.Default
    @Schema(description = "Priority level", example = "HIGH", allowableValues = {"HIGH", "MEDIUM", "LOW"})
    private String priority = "MEDIUM";
    
    @Schema(description = "Task category", example = "LOGISTICS", 
            allowableValues = {"LOGISTICS", "MARKETING", "CATERING", "VENUE", "STAFF", "TECHNICAL", "OTHER"})
    private String category;
    
    @Schema(description = "User ID assigned to this task")
    private UUID assignedTo;
    
    @Builder.Default
    @Schema(description = "Initial status", example = "TO_DO")
    private TimelineStatus status = TimelineStatus.TO_DO;
    
    @Schema(description = "Parent task ID if this is a subtask")
    private UUID parentTaskId;
    
    @Schema(description = "Order of this task among siblings")
    private Integer taskOrder;
    
    @Builder.Default
    @Schema(description = "Whether this task is in preview/draft state")
    private Boolean isPreview = false;
    
    @Schema(description = "List of dependency task IDs")
    private List<UUID> dependencies;
    
    @Valid
    @Schema(description = "List of subtasks to create with this parent task")
    private List<CreateSubtaskRequest> subtasks;
    
    /**
     * Request DTO for creating a subtask
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to create a subtask")
    public static class CreateSubtaskRequest {
        
        @NotBlank(message = "Subtask title is required")
        @Size(max = 255, message = "Subtask title must not exceed 255 characters")
        private String title;
        
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        private String description;
        
        private LocalDateTime startDate;
        private LocalDateTime dueDate;
        private Integer durationMinutes;
        
        @Builder.Default
        private String priority = "MEDIUM";
        
        private UUID assignedTo;
        
        @Builder.Default
        private TimelineStatus status = TimelineStatus.TO_DO;
        
        private Integer taskOrder;
        
        @Builder.Default
        private Boolean isPreview = false;
    }
}

