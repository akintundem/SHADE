package eventplanner.features.event.enums;

/**
 * Defines the recurrence pattern for event series.
 * Determines how often events in a series repeat.
 */
public enum RecurrencePattern {
    
    /**
     * Event repeats every N days.
     */
    DAILY,
    
    /**
     * Event repeats every N weeks on specified day(s).
     */
    WEEKLY,
    
    /**
     * Event repeats every N months on a specific day or nth weekday.
     */
    MONTHLY,
    
    /**
     * Event repeats every N years on a specific date.
     */
    YEARLY,
    
    /**
     * Custom recurrence pattern with specific dates.
     * Dates are stored separately in the series configuration.
     */
    CUSTOM
}
