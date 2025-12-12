package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO for sharing an event
 */
@Schema(description = "Event sharing request")
@Getter
@Setter
public class EventShareRequest {

    @NotBlank(message = "Channel is required")
    @Schema(description = "Sharing channel (email, sms, social, link)", example = "email")
    private String channel;

    @Schema(description = "List of recipient email addresses or phone numbers")
    private List<String> recipients;

    @Schema(description = "Custom message to include with the share")
    private String message;

    @Schema(description = "Whether to include event details in the share")
    private Boolean includeEventDetails = true;

    @Schema(description = "Expiration date for the share link (if applicable)")
    private String expirationDate;

    @Schema(description = "Whether the share requires authentication to view")
    private Boolean requiresAuth = false;
}
