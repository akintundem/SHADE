package ai.eventplanner.common.domain.enums;

/**
 * Vendor relationship status
 */
public enum VendorStatus {
    INQUIRY,        // Initial inquiry sent
    RFP_SENT,       // Request for proposal sent
    QUOTED,         // Quote received
    NEGOTIATING,    // Negotiating terms
    BOOKED,         // Confirmed booking
    IN_PROGRESS,    // Service in progress
    COMPLETED,      // Service completed
    CANCELLED,      // Booking cancelled
    NO_RESPONSE     // No response from vendor
}
