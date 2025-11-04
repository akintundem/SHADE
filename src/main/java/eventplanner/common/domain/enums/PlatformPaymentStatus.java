package eventplanner.common.domain.enums;

/**
 * Platform payment status for event creation fees
 */
public enum PlatformPaymentStatus {
    PENDING,           // Payment initiated
    PROCESSING,        // Payment being processed
    COMPLETED,         // Payment successful
    FAILED,            // Payment failed
    REFUNDED,          // Payment refunded
    PARTIALLY_REFUNDED, // Partial refund
    CANCELLED,          // Payment cancelled
    EXPIRED            // Payment expired
}
