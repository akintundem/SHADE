package eventplanner.features.timeline.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for generating workback schedule
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to generate workback schedule from event date")
public class WorkbackRequest {

    @NotBlank(message = "Event date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Event date must be in YYYY-MM-DD format")
    @Schema(description = "Event date in ISO-8601 format", example = "2024-06-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private String eventDate;

    @Schema(description = "Event type for customized milestones", example = "wedding")
    private String eventType;

    @Schema(description = "Event size for customized timeline", example = "large")
    private String eventSize;

    @Schema(description = "Custom milestone preferences")
    private String customPreferences;
}
