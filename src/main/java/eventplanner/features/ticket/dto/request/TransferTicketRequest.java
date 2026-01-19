package eventplanner.features.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "Transfer ticket ownership to another attendee or email")
public class TransferTicketRequest {

    @Schema(description = "New attendee ID (must belong to the same event)")
    private UUID newAttendeeId;

    @Email(message = "Valid email is required")
    @Schema(description = "New ticket holder email (for email-only tickets)")
    private String newOwnerEmail;

    @Schema(description = "New ticket holder name (required with newOwnerEmail)")
    private String newOwnerName;

    @Schema(description = "Send email notification to new owner")
    private Boolean sendEmail = true;

    @Schema(description = "Send push notification to new owner (when attendee is linked)")
    private Boolean sendPush = true;
}
