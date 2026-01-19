package eventplanner.features.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for validating a ticket via QR code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTicketRequest {

    @NotBlank(message = "QR code data is required")
    private String qrCodeData;

    @NotNull(message = "Event ID is required for validation context")
    private UUID eventId;
}

