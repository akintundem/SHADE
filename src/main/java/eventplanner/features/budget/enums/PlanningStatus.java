package eventplanner.features.budget.enums;

/**
 * Planning status enumeration for budget items
 */
public enum PlanningStatus {
    PLANNED,        // Initial planning phase
    QUOTED,         // Quote received
    BOOKED,         // Service confirmed and booked
    IN_PROGRESS,    // Service in progress
    COMPLETED,      // Service completed
    CANCELLED,      // Planning cancelled
    ON_HOLD         // Planning on hold
}
