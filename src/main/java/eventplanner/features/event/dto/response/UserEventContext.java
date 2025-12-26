package eventplanner.features.event.dto.response;

import eventplanner.common.domain.enums.UserEventAccessStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User-specific context for an event.
 * This DTO provides all information the mobile app needs to render
 * the appropriate UI based on the user's relationship with the event.
 */
@Schema(description = "User's relationship and access context for an event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEventContext {

    // ============ ACCESS STATUS ============
    
    @Schema(description = "User's current access status for this event", 
            example = "TICKET_PURCHASED")
    private UserEventAccessStatus accessStatus;

    @Schema(description = "Whether the user currently has access to view event content/feeds", 
            example = "true")
    private Boolean hasAccess;

    @Schema(description = "Human-readable message explaining the access status", 
            example = "You have a valid ticket for this event")
    private String accessMessage;

    // ============ USER ROLE ============
    
    @Schema(description = "Whether the user is the event owner", 
            example = "false")
    private Boolean isOwner;

    @Schema(description = "Whether the user is a collaborator/organizer", 
            example = "false")
    private Boolean isCollaborator;

    @Schema(description = "User's role in the event if they have one", 
            example = "COORDINATOR")
    private String eventRole;

    // ============ RSVP INFORMATION ============
    
    @Schema(description = "Whether the user has RSVP'd to this event", 
            example = "true")
    private Boolean hasRsvp;

    @Schema(description = "RSVP status if user has RSVP'd", 
            example = "CONFIRMED")
    private String rsvpStatus;

    @Schema(description = "When the user RSVP'd", 
            example = "2024-01-15T10:30:00")
    private LocalDateTime rsvpAt;

    // ============ INVITE INFORMATION ============
    
    @Schema(description = "Whether the user has been invited to this event", 
            example = "false")
    private Boolean hasInvite;

    @Schema(description = "Invite status if user has been invited", 
            example = "PENDING")
    private String inviteStatus;

    @Schema(description = "When the invite was sent", 
            example = "2024-01-10T14:00:00")
    private LocalDateTime invitedAt;

    @Schema(description = "Who sent the invite (name)", 
            example = "John Doe")
    private String invitedByName;

    // ============ TICKET INFORMATION ============
    
    @Schema(description = "Whether the user has any ticket (valid or not) for this event", 
            example = "true")
    private Boolean hasTicket;

    @Schema(description = "Whether the user has a valid (ISSUED or VALIDATED) ticket", 
            example = "true")
    private Boolean hasValidTicket;

    @Schema(description = "Number of valid tickets the user has", 
            example = "2")
    private Integer ticketCount;

    @Schema(description = "Primary ticket ID if user has a ticket")
    private UUID primaryTicketId;

    @Schema(description = "Primary ticket number if user has a ticket", 
            example = "TKT-2024-00001")
    private String primaryTicketNumber;

    @Schema(description = "Primary ticket type name", 
            example = "VIP Access")
    private String primaryTicketTypeName;

    @Schema(description = "Primary ticket status", 
            example = "ISSUED")
    private String primaryTicketStatus;

    @Schema(description = "When the primary ticket was issued", 
            example = "2024-01-20T09:00:00")
    private LocalDateTime ticketIssuedAt;

    // ============ PAYMENT INFORMATION (for ticketed events) ============
    
    @Schema(description = "Whether payment is required for this event", 
            example = "true")
    private Boolean requiresPayment;

    @Schema(description = "Whether the user has completed payment", 
            example = "true")
    private Boolean hasPaid;

    @Schema(description = "Total amount paid by user for tickets", 
            example = "150.00")
    private BigDecimal amountPaid;

    @Schema(description = "Currency of payment", 
            example = "USD")
    private String paymentCurrency;

    // ============ CHECK-IN INFORMATION ============
    
    @Schema(description = "Whether the user has checked in to the event", 
            example = "false")
    private Boolean hasCheckedIn;

    @Schema(description = "When the user checked in", 
            example = "2024-02-01T18:30:00")
    private LocalDateTime checkedInAt;

    // ============ ACTION HINTS FOR FRONTEND ============
    
    @Schema(description = "Whether 'Buy Ticket' action should be shown", 
            example = "false")
    private Boolean canBuyTicket;

    @Schema(description = "Whether 'RSVP' action should be shown", 
            example = "true")
    private Boolean canRsvp;

    @Schema(description = "Whether 'Accept Invite' action should be shown", 
            example = "false")
    private Boolean canRespondToInvite;

    @Schema(description = "Whether 'View Feeds' action should be shown", 
            example = "true")
    private Boolean canViewFeeds;

    @Schema(description = "Whether 'Express Interest' action should be shown", 
            example = "false")
    private Boolean canExpressInterest;

    @Schema(description = "Primary call-to-action for the UI", 
            example = "VIEW_FEEDS")
    private String primaryAction;

    @Schema(description = "Label for the primary action button", 
            example = "View Event")
    private String primaryActionLabel;
}

