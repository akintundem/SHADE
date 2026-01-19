package eventplanner.features.timeline.dto.request;

import eventplanner.features.timeline.enums.TimelineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for auto-saving a task draft
 */
@Data
@Schema(description = "Request to auto-save a task draft")
public class TaskAutoSaveRequest {
    
    @Schema(description = "Task ID (null for new task)")
    private UUID id;
    
    @Size(max = 255)
    @Schema(description = "Task title")
    private String title;
    
    @Size(max = 2000)
    @Schema(description = "Task description")
    private String description;
    
    @Schema(description = "Task start date")
    private LocalDateTime startDate;
    
    @Schema(description = "Task due date")
    private LocalDateTime dueDate;
    
    @Schema(description = "Priority level")
    private String priority;
    
    @Schema(description = "Task category")
    private String category;
    
    @Schema(description = "User ID assigned to this task")
    private UUID assignedTo;
    
    @Schema(description = "Task status")
    private TimelineStatus status;
    
    @Schema(description = "Order of this task")
    private Integer taskOrder;
}

