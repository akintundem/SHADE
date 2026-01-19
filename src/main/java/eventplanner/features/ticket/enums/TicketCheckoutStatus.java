package eventplanner.features.ticket.enums;

/**
 * Checkout lifecycle states for ticket purchases.
 */
public enum TicketCheckoutStatus {
    /**
     * Checkout created and tickets reserved, awaiting payment/confirmation.
     */
    PENDING_PAYMENT,
    /**
     * Checkout completed and tickets issued.
     */
    COMPLETED,
    /**
     * Checkout was cancelled before completion.
     */
    CANCELLED,
    /**
     * Checkout expired before completion (reservations released).
     */
    EXPIRED
}
