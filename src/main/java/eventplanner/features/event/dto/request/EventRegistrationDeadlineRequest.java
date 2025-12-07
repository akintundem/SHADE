package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request DTO for updating event registration deadline
 */
@Schema(description = "Request to update event registration deadline")
@Getter
@Setter
public class EventRegistrationDeadlineRequest {

    @NotNull(message = "Registration deadline is required")
    @Schema(description = "New registration deadline", example = "2024-06-10T23:59:59", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime deadline;
}
