package eventplanner.features.attendee.enums;

/**
 * RSVP status enumeration for attendees.
 * Maps to the attendees table rsvp_status column using EnumType.STRING
 */
public enum AttendeeStatus {
    PENDING,
    CONFIRMED,
    DECLINED,
    TENTATIVE,
    NO_SHOW
}

