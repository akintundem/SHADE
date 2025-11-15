package eventplanner.common.domain.enums;

/**
 * Event scope enumeration - determines what level of event data a user can access
 */
public enum EventScope {
    /**
     * Full event details - for owners and high-responsibility roles
     */
    FULL,
    
    /**
     * Feed view - for guests and low-responsibility roles
     */
    FEED
}

