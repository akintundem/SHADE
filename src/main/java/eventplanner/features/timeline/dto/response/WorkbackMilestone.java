package eventplanner.features.timeline.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO for workback milestone
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Workback milestone item")
public class WorkbackMilestone {

    @Schema(description = "Milestone title", example = "Send Save-the-Dates")
    private String title;

    @Schema(description = "Days before event (negative number)", example = "-60")
    private Integer dueInDays;

    @Schema(description = "Calculated due date", example = "2024-04-15")
    private LocalDate dueDate;

    @Schema(description = "Milestone priority", example = "high")
    private String priority;

    @Schema(description = "Milestone category", example = "communication")
    private String category;

    @Schema(description = "Additional milestone details")
    private String description;
}
