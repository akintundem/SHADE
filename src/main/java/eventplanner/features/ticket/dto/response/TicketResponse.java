package eventplanner.features.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eventplanner.features.ticket.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for ticket information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketResponse {

    private UUID id;
    private String ticketNumber;
    private UUID eventId;
    private String eventName;
    private UUID ticketTypeId;
    private String ticketTypeName;
    private UUID attendeeId;
    private String attendeeName;
    private String attendeeEmail;
    private TicketStatus status;
    private String qrCodeData;
    private String qrCodeImageBase64; // Base64 encoded QR code image
    private String qrCodeImageUrl; // URL to QR code image if stored externally
    private TicketWalletResponse wallet;
    private LocalDateTime pendingAt; // When ticket entered PENDING status
    private LocalDateTime pendingExpirationTime; // When pending ticket expires (15 minutes after pendingAt)
    private LocalDateTime issuedAt;
    private LocalDateTime validatedAt;
    private Boolean canBeValidated; // Computed: true if ticket can be validated
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
