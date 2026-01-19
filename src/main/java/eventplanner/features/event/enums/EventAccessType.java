package eventplanner.features.event.enums;

/**
 * Defines how users can access an event's content and participate.
 * This determines the requirements for viewing feeds and attending the event.
 */
public enum EventAccessType {
    
    /**
     * Open/Public event - Anyone can view content and RSVP.
     * No restrictions on who can see the event or its feeds.
     */
    OPEN,
    
    /**
     * RSVP Required - Users must RSVP to access event content.
     * Event is visible to all, but feeds/content require confirmed RSVP.
     * Approval may or may not be required based on event settings.
     */
    RSVP_REQUIRED,
    
    /**
     * Invite Only - Users must be invited by owner/collaborators.
     * Event is not publicly discoverable. Only invited users can see it.
     * Invites can be sent by owner, collaborators, or authorized users.
     */
    INVITE_ONLY,
    
    /**
     * Ticketed - Users must purchase a ticket to access content.
     * Event may be publicly visible, but feeds/content require a valid ticket.
     * Free tickets (price = 0) still require ticket acquisition.
     */
    TICKETED
}
