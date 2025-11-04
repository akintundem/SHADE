package eventplanner.common.domain.enums;

/**
 * Quote status enumeration for vendor quotes
 */
public enum QuoteStatus {
    REQUESTED,      // Quote requested from vendor
    RECEIVED,       // Quote received
    REVIEWED,       // Quote under review
    ACCEPTED,       // Quote accepted
    REJECTED,       // Quote rejected
    EXPIRED,        // Quote expired
    NEGOTIATING     // Negotiating terms
}
