package eventplanner.features.ticket.enums;

/**
 * Ticket status enumeration for ticket lifecycle management.
 * Maps to the tickets table status column using EnumType.STRING
 */
public enum TicketStatus {
    /**
     * Ticket created but not yet issued (awaiting payment for paid tickets)
     */
    PENDING,
    
    /**
     * Ticket is active and valid for use
     */
    ISSUED,
    
    /**
     * Ticket has been scanned/validated at event
     */
    VALIDATED,
    
    /**
     * Ticket was cancelled before validation
     */
    CANCELLED,
    
    /**
     * Ticket was refunded (for paid tickets)
     */
    REFUNDED
}

