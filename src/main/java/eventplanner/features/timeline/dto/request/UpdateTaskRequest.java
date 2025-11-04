package eventplanner.features.timeline.dto.request;

import eventplanner.common.domain.enums.TimelineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating a task
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a task")
public class UpdateTaskRequest {
    
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Task title", example = "Venue Setup")
    private String title;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Task description")
    private String description;
    
    @Schema(description = "Task start date and time")
    private LocalDateTime startDate;
    
    @Schema(description = "Task due date")
    private LocalDateTime dueDate;
    
    @Schema(description = "Duration in minutes")
    private Integer durationMinutes;
    
    @Schema(description = "Priority level", example = "HIGH")
    private String priority;
    
    @Schema(description = "Task category", example = "LOGISTICS")
    private String category;
    
    @Schema(description = "User ID assigned to this task")
    private UUID assignedTo;
    
    @Schema(description = "Task status")
    private TimelineStatus status;
    
    @Min(value = 0, message = "Progress must be between 0 and 100")
    @Max(value = 100, message = "Progress must be between 0 and 100")
    @Schema(description = "Progress percentage (0-100)", example = "50")
    private Integer progressPercentage;
    
    @Schema(description = "Whether this task is in preview/draft state")
    private Boolean isPreview;
    
    @Schema(description = "Parent task ID (to move task under different parent)")
    private UUID parentTaskId;
    
    @Schema(description = "Order of this task among siblings")
    private Integer taskOrder;
    
    @Schema(description = "List of dependency task IDs")
    private List<UUID> dependencies;
    
    @Schema(description = "Payment date")
    private LocalDateTime paymentDate;
    
    @Schema(description = "Proof image URL (single image)")
    private String proofImageUrl;
    
    @Schema(description = "Proof image URLs (multiple images as JSON array)")
    private List<String> proofImageUrls;
}

