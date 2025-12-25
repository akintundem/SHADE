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
 * For wallet pass data, use the dedicated GET /{id}/wallet-pass endpoint.
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
    private LocalDateTime pendingAt;
    private LocalDateTime pendingExpirationTime;
    private LocalDateTime issuedAt;
    private LocalDateTime validatedAt;
    private Boolean canBeValidated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
