package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request DTO for manually generating series occurrences.
 */
@Schema(description = "Request to generate occurrences for an event series")
@Getter
@Setter
public class GenerateOccurrencesRequest {

    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 100, message = "Cannot generate more than 100 occurrences at once")
    @Schema(description = "Number of occurrences to generate", example = "4")
    private Integer count;

    @Schema(description = "Generate occurrences up to this date (alternative to count)", 
            example = "2024-12-31T23:59:59")
    private LocalDateTime untilDate;

    @Schema(description = "Whether to skip conflicting dates (holidays, etc.)", 
            example = "false", defaultValue = "false")
    private Boolean skipConflicts = false;

    @Schema(description = "Whether to force generation even if series has reached its limit", 
            example = "false", defaultValue = "false")
    private Boolean forceGeneration = false;
}
