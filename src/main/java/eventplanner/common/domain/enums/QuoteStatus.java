package eventplanner.common.domain.enums;

/**
 * Quote status enumeration
 */
public enum QuoteStatus {
    REQUESTED,      // Quote requested
    RECEIVED,       // Quote received
    REVIEWED,       // Quote under review
    ACCEPTED,       // Quote accepted
    REJECTED,       // Quote rejected
    EXPIRED,        // Quote expired
    NEGOTIATING     // Negotiating terms
}
