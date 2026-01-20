package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for joining an event waitlist.
 */
@Schema(description = "Request to join an event waitlist")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventWaitlistRequest {

    @Schema(description = "Email address (required if not authenticated)", example = "user@example.com")
    private String email;

    @Schema(description = "Full name (required if not authenticated)", example = "John Doe")
    private String name;
}
