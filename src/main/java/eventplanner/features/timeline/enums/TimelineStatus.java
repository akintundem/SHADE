package eventplanner.features.timeline.enums;

/**
 * Timeline item status enumeration
 */
public enum TimelineStatus {
    PENDING,
    TO_DO,      // Task not yet started
    ACTIVE,     // Task currently in progress
    IN_PROGRESS, // Alias for ACTIVE
    COMPLETED,
    DONE,       // Alias for COMPLETED
    CANCELLED,
    POSTPONED,
    OVERDUE
}
