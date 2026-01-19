package eventplanner.features.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Update ticket holder details")
public class UpdateTicketRequest {

    @Email(message = "Valid email is required")
    @Schema(description = "Ticket holder email")
    private String ownerEmail;

    @Schema(description = "Ticket holder name")
    private String ownerName;
}
