package eventplanner.features.event.enums;

/**
 * Enumeration for email template types used in event notifications.
 * Each value maps 1-to-1 with a template registered in the email service
 * (see email/templates/index.ts for the canonical template registry).
 */
public enum EmailTemplateType {

    /** General announcement to event participants. */
    ANNOUNCEMENT,

    /** Event cancellation notice. */
    CANCEL_EVENT,

    /** Scheduled reminder sent before an event starts. */
    EVENT_REMINDER,

    /** Welcome email sent to an attendee after they register. */
    ATTENDEE_WELCOME,

    /** Invitation sent to a prospective attendee. */
    ATTENDEE_INVITE,

    /** Notification to the organiser when an invitee responds. */
    ATTENDEE_INVITE_RESPONSE,

    /** Ticket confirmation sent after a successful ticket purchase. */
    TICKET_CONFIRMATION,

    /** Invitation sent to a prospective collaborator. */
    COLLABORATOR_INVITE,

    /** Welcome email sent to a collaborator after they join the event. */
    COLLABORATOR_WELCOME
}
