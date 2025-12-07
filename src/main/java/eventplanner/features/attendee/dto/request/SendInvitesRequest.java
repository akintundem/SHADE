package eventplanner.features.attendee.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for sending invitations to attendees
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to send invitations to attendees")
public class SendInvitesRequest {

    @NotNull(message = "Event ID is required")
    @Schema(description = "Event ID", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID eventId;

    @NotEmpty(message = "Attendee IDs list cannot be empty")
    @Schema(description = "List of attendee IDs to send invitations to", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UUID> attendeeIds;

    @Schema(description = "Custom message to include in the invitation", example = "You're invited to our special event!")
    private String customMessage;

    @Schema(description = "Send via email", example = "true")
    private Boolean sendEmail = true;

    @Schema(description = "Send via push notification", example = "false")
    private Boolean sendPush = false;
}
