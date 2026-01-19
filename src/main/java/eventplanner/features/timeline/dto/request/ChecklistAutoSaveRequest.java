package eventplanner.features.timeline.dto.request;

import eventplanner.features.timeline.enums.TimelineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for auto-saving a checklist item draft
 */
@Data
@Schema(description = "Request to auto-save a checklist item draft")
public class ChecklistAutoSaveRequest {
    
    @Schema(description = "Checklist item ID (null for new item)")
    private UUID id;
    
    @Size(max = 255)
    @Schema(description = "Checklist item title")
    private String title;
    
    @Size(max = 2000)
    @Schema(description = "Checklist item description")
    private String description;
    
    @Schema(description = "Due date")
    private LocalDateTime dueDate;
    
    @Schema(description = "Assigned user ID")
    private UUID assignedTo;
    
    @Schema(description = "Status")
    private TimelineStatus status;
    
    @Schema(description = "Order among subtasks")
    private Integer taskOrder;
}

