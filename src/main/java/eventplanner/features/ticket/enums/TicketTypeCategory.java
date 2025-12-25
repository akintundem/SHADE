package eventplanner.features.ticket.enums;

/**
 * Ticket type category enumeration.
 * Defines common categories for ticket types (VIP, General Admission, etc.).
 */
public enum TicketTypeCategory {
    /**
     * General admission - standard entry ticket
     */
    GENERAL_ADMISSION,
    
    /**
     * VIP ticket - premium access with additional benefits
     */
    VIP,
    
    /**
     * Early bird - discounted tickets for early purchasers
     */
    EARLY_BIRD,
    
    /**
     * Student ticket - discounted for students
     */
    STUDENT,
    
    /**
     * Senior ticket - discounted for seniors
     */
    SENIOR,
    
    /**
     * Child ticket - discounted for children
     */
    CHILD,
    
    /**
     * Group ticket - for group purchases
     */
    GROUP,
    
    /**
     * Premium ticket - high-tier access
     */
    PREMIUM,
    
    /**
     * Backstage pass - access to backstage areas
     */
    BACKSTAGE,
    
    /**
     * Meet and greet - includes meet and greet with performers/speakers
     */
    MEET_AND_GREET,
    
    /**
     * All access pass - full event access
     */
    ALL_ACCESS,
    
    /**
     * Single day pass - for multi-day events
     */
    SINGLE_DAY,
    
    /**
     * Weekend pass - for weekend events
     */
    WEEKEND_PASS,
    
    /**
     * Season pass - for recurring events
     */
    SEASON_PASS,
    
    /**
     * Standing room only - no assigned seating
     */
    STANDING_ROOM,
    
    /**
     * Reserved seating - assigned seats
     */
    RESERVED_SEATING,
    
    /**
     * Other category - custom category
     */
    OTHER
}

