package eventplanner.features.attendee.entity;

/**
 * RSVP status enumeration for attendees
 * Maps to the attendees table rsvp_status column
 */
public enum AttendeeStatus {
    PENDING("pending"),
    CONFIRMED("confirmed"),
    DECLINED("declined"),
    TENTATIVE("tentative"),
    NO_SHOW("no_show");
    
    private final String value;
    
    AttendeeStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Parse from string value (case-insensitive)
     */
    public static AttendeeStatus fromString(String value) {
        if (value == null) {
            return PENDING;
        }
        
        String normalized = value.toLowerCase().trim();
        for (AttendeeStatus status : AttendeeStatus.values()) {
            if (status.value.equals(normalized) || status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Invalid attendee status: " + value + 
            ". Valid values are: pending, confirmed, declined, tentative, no_show");
    }
    
    /**
     * Check if a string is a valid status
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        
        try {
            fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

