package eventplanner.features.event.enums;

/**
 * Defines how an event series ends.
 */
public enum RecurrenceEndType {
    
    /**
     * Series ends on a specific date.
     * Uses recurrenceEndDate field.
     */
    BY_DATE,
    
    /**
     * Series ends after a specific number of occurrences.
     * Uses maxOccurrences field.
     */
    BY_OCCURRENCES,
    
    /**
     * Series has no end date (continues indefinitely).
     * Occurrences are generated on-demand up to a configurable future horizon.
     */
    NEVER
}
