package ai.eventplanner.common.domain.enums;

/**
 * Planning status enumeration for budget items and vendor relationships
 */
public enum PlanningStatus {
    PLANNED,        // Initial planning phase
    QUOTED,         // Quote received from vendor
    BOOKED,         // Vendor confirmed and booked
    IN_PROGRESS,    // Service in progress
    COMPLETED,      // Service completed
    CANCELLED,      // Planning cancelled
    ON_HOLD         // Planning on hold
}
