package eventplanner.features.event.enums;

/**
 * Enumeration for recipient types in bulk notifications and reminders
 */
public enum RecipientType {
    /**
     * Send to all collaborators (EventUser with type COLLABORATOR, ORGANIZER, COORDINATOR, etc.)
     */
    ALL_COLLABORATORS,
    
    /**
     * Send to all vendors (EventUser with type VENDOR)
     */
    ALL_VENDORS,
    
    /**
     * Send to all guests/attendees (Attendee entities)
     */
    ALL_GUESTS,
    
    /**
     * Send to a specific person (requires recipientUserIds or recipientEmails)
     */
    SPECIFIC_PERSON
}
