package eventplanner.features.timeline.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for bulk updating multiple tasks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to bulk update multiple tasks")
public class BulkUpdateTaskRequest {
    
    @NotEmpty(message = "At least one update is required")
    @Schema(description = "List of task updates", required = true)
    private List<@Valid TaskUpdate> updates;
    
    /**
     * Individual task update entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual task update")
    public static class TaskUpdate {
        
        @NotNull(message = "Task ID is required")
        @Schema(description = "Task ID to update", required = true)
        private UUID taskId;
        
        @Schema(description = "New start date")
        private LocalDateTime startDate;
        
        @Schema(description = "New end date")
        private LocalDateTime endDate;
        
        @Schema(description = "New duration in minutes")
        private Integer durationMinutes;
        
        @Schema(description = "New status")
        private String status;
    }
}


