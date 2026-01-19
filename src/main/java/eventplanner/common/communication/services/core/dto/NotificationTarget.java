package eventplanner.common.communication.services.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a notification target with user IDs and emails
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTarget {
    
    private TargetType targetType;
    private Set<UUID> userIds;
    private Set<String> emails;
    
    public enum TargetType {
        /**
         * All users in the system
         */
        ALL_USERS,
        
        /**
         * All attendees of a specific event (including those with user accounts)
         */
        EVENT_ATTENDEES,
        
        /**
         * All guests (external attendees without user accounts) of a specific event
         */
        EVENT_GUESTS,
        
        /**
         * All collaborators (organizers, coordinators, etc.) of a specific event
         */
        EVENT_COLLABORATORS,
        
        /**
         * Attendees who have checked in to an event
         */
        EVENT_CHECKED_IN,
        
        /**
         * Attendees who have not checked in to an event
         */
        EVENT_NOT_CHECKED_IN,
        
        /**
         * Specific users by their IDs
         */
        SPECIFIC_USERS,
        
        /**
         * Users matching a specific segment/criteria
         */
        USER_SEGMENT
    }
    
    public int getTotalCount() {
        return (userIds != null ? userIds.size() : 0) + (emails != null ? emails.size() : 0);
    }
    
    public boolean isEmpty() {
        return (userIds == null || userIds.isEmpty()) && (emails == null || emails.isEmpty());
    }
}

