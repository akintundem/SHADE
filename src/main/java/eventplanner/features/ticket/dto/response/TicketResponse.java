package eventplanner.features.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eventplanner.features.ticket.entity.Ticket;
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
    private UUID checkoutId;
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

    /**
     * Create a TicketResponse from a Ticket entity.
     * Handles lazy loading exceptions gracefully.
     */
    public static TicketResponse from(Ticket ticket) {
        TicketResponse.TicketResponseBuilder builder = TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus())
                .qrCodeData(ticket.getQrCodeData())
                .pendingAt(ticket.getPendingAt())
                .pendingExpirationTime(ticket.getPendingExpirationTime())
                .issuedAt(ticket.getIssuedAt())
                .validatedAt(ticket.getValidatedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt());
        
        // Safely extract event information
        try {
            if (ticket.getEvent() != null) {
                builder.eventId(ticket.getEvent().getId());
                builder.eventName(ticket.getEvent().getName());
            }
        } catch (Exception e) {
            // Lazy loading exception - event not loaded
        }
        
        // Safely extract ticket type information
        try {
            if (ticket.getTicketType() != null) {
                builder.ticketTypeId(ticket.getTicketType().getId());
                builder.ticketTypeName(ticket.getTicketType().getName());
            }
        } catch (Exception e) {
            // Lazy loading exception - ticket type not loaded
        }
        
        // Safely extract checkout information
        try {
            if (ticket.getCheckout() != null) {
                builder.checkoutId(ticket.getCheckout().getId());
            }
        } catch (Exception e) {
            // Lazy loading exception - checkout not loaded
        }
        
        // Safely extract attendee information
        try {
            if (ticket.getAttendee() != null) {
                builder.attendeeId(ticket.getAttendee().getId());
                builder.attendeeName(ticket.getAttendee().getName());
                builder.attendeeEmail(ticket.getAttendee().getEmail());
            } else {
                builder.attendeeName(ticket.getOwnerName());
                builder.attendeeEmail(ticket.getOwnerEmail());
            }
        } catch (Exception e) {
            // Lazy loading exception - use owner info as fallback
            builder.attendeeName(ticket.getOwnerName());
            builder.attendeeEmail(ticket.getOwnerEmail());
        }
        
        // Safely check if ticket can be validated
        builder.canBeValidated(safelyCheckCanBeValidated(ticket));
        
        return builder.build();
    }

    /**
     * Safely check if ticket can be validated, handling potential lazy loading issues.
     */
    private static Boolean safelyCheckCanBeValidated(Ticket ticket) {
        try {
            return ticket.canBeValidated();
        } catch (Exception e) {
            // If there's a lazy loading issue or other exception, return null
            // This prevents 500 errors when event relationship is not loaded
            return null;
        }
    }
}
