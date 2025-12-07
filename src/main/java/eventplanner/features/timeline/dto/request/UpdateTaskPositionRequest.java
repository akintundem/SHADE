package eventplanner.features.timeline.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for updating task position on timeline (drag-and-drop)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update task position on timeline")
public class UpdateTaskPositionRequest {
    
    @NotNull(message = "Start date is required")
    @Schema(description = "New start date and time", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-05-09T10:00:00")
    private LocalDateTime startDate;
    
    @Schema(description = "New end date and time", example = "2024-05-12T18:00:00")
    private LocalDateTime endDate;
    
    @Schema(description = "Duration in minutes (calculated if not provided)", example = "2880")
    private Integer durationMinutes;
}


