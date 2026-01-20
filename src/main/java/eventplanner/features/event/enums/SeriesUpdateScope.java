package eventplanner.features.event.enums;

/**
 * Defines the scope of updates when modifying an event series or individual occurrence.
 */
public enum SeriesUpdateScope {
    
    /**
     * Update only this specific occurrence.
     * Detaches the occurrence from series settings for the updated fields.
     */
    THIS_OCCURRENCE_ONLY,
    
    /**
     * Update this occurrence and all future occurrences.
     * Past occurrences remain unchanged.
     */
    THIS_AND_FUTURE,
    
    /**
     * Update all occurrences in the series (past and future).
     * Use with caution - may affect historical data.
     */
    ALL_OCCURRENCES
}
