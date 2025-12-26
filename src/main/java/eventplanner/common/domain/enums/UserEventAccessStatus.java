package eventplanner.common.domain.enums;

/**
 * Represents the current user's access status relative to an event.
 * This is computed per-user at query time and used by the frontend to render appropriate UI.
 */
public enum UserEventAccessStatus {
    
    // ============ OWNER / STAFF STATUSES ============
    
    /**
     * User is the event owner - full access to everything.
     */
    OWNER,
    
    /**
     * User is a collaborator/organizer - has management access.
     */
    COLLABORATOR,
    
    // ============ OPEN EVENT STATUSES ============
    
    /**
     * User has not interacted with this open event yet.
     * Can freely view content and RSVP if desired.
     */
    OPEN_NOT_REGISTERED,
    
    /**
     * User has RSVP'd to an open event.
     */
    OPEN_RSVP_CONFIRMED,
    
    // ============ RSVP-REQUIRED EVENT STATUSES ============
    
    /**
     * User has not RSVP'd to an RSVP-required event.
     * Cannot view full content until RSVP is confirmed.
     */
    RSVP_NOT_REGISTERED,
    
    /**
     * User has submitted RSVP but it's pending approval.
     */
    RSVP_PENDING_APPROVAL,
    
    /**
     * User's RSVP has been confirmed/approved - has access.
     */
    RSVP_CONFIRMED,
    
    /**
     * User's RSVP was declined.
     */
    RSVP_DECLINED,
    
    // ============ INVITE-ONLY EVENT STATUSES ============
    
    /**
     * User has not been invited to this invite-only event.
     * Should not normally see this event in listings.
     */
    INVITE_NOT_INVITED,
    
    /**
     * User has been invited but hasn't responded yet.
     */
    INVITE_PENDING_RESPONSE,
    
    /**
     * User accepted the invite - has access.
     */
    INVITE_ACCEPTED,
    
    /**
     * User declined the invite.
     */
    INVITE_DECLINED,
    
    // ============ TICKETED EVENT STATUSES ============
    
    /**
     * User has not purchased a ticket for this ticketed event.
     */
    TICKET_NOT_PURCHASED,
    
    /**
     * User has a pending ticket (payment not completed or awaiting approval).
     */
    TICKET_PENDING,
    
    /**
     * User has a valid issued ticket - has access.
     */
    TICKET_PURCHASED,
    
    /**
     * User's ticket has been validated/used at the event.
     */
    TICKET_VALIDATED,
    
    /**
     * User's ticket was cancelled or refunded.
     */
    TICKET_CANCELLED,
    
    // ============ POST-EVENT STATUSES ============
    
    /**
     * Event has ended and content is now publicly accessible.
     * Applies when feedsPublicAfterEvent = true.
     */
    EVENT_ENDED_PUBLIC,
    
    /**
     * Event has ended but user still has access (was attendee/ticket holder).
     */
    EVENT_ENDED_WITH_ACCESS,
    
    /**
     * Event has ended and user does not have access to content.
     */
    EVENT_ENDED_NO_ACCESS,
    
    // ============ SPECIAL STATUSES ============
    
    /**
     * User is not authenticated - access depends on event settings.
     */
    ANONYMOUS,
    
    /**
     * Access status could not be determined.
     */
    UNKNOWN
}

