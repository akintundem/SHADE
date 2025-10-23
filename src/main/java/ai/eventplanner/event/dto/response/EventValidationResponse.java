package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for event validation results
 */
@Schema(description = "Event validation response")
@Getter
@Setter
public class EventValidationResponse {

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Overall validation status")
    private Boolean isValid;

    @Schema(description = "Validation score (0-100)")
    private Integer validationScore;

    @Schema(description = "List of validation errors")
    private List<String> errors;

    @Schema(description = "List of validation warnings")
    private List<String> warnings;

    @Schema(description = "Validation details by category")
    private Map<String, Object> validationDetails;

    @Schema(description = "Validation timestamp")
    private String validatedAt;
}
