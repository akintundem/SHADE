package eventplanner.features.attendee.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eventplanner.features.attendee.enums.AttendeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sanitized attendee response that doesn't leak internal JPA fields.
 * 
 * Attendees can be either:
 * - User-linked: Added by userId, has both userId and email (from user account)
 * - Email-only guest: Added by email only, has email but userId is null
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendeeResponse {
    
    private UUID id;
    private UUID eventId;
    private UUID userId; // User account ID if attendee is linked to a user in the platform (null for email-only guests)
    private String name;
    private String email; // Email address - present for both user-linked attendees and email-only guests
    
    // Status information
    private AttendeeStatus rsvpStatus;
    private LocalDateTime checkedInAt;
    
    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Helper method to check if attendee is checked in
     * Computed dynamically from checkedInAt
     */
    public Boolean getIsCheckedIn() {
        return checkedInAt != null;
    }
}
