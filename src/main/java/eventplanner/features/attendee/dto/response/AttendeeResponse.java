package eventplanner.features.attendee.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eventplanner.common.domain.enums.VisibilityLevel;
import eventplanner.features.attendee.entity.Attendee;
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
    
    // Visibility setting
    private VisibilityLevel participationVisibility;
    
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

    /**
     * Create an AttendeeResponse from an Attendee entity.
     * Handles both user-linked attendees (with userId) and email-only guests (userId is null).
     */
    public static AttendeeResponse from(Attendee attendee) {
        return AttendeeResponse.builder()
                .id(attendee.getId())
                .eventId(attendee.getEvent() != null ? attendee.getEvent().getId() : null)
                .userId(attendee.getUser() != null ? attendee.getUser().getId() : null)
                .name(attendee.getName())
                .email(attendee.getEmail())
                .rsvpStatus(attendee.getRsvpStatus())
                .checkedInAt(attendee.getCheckedInAt())
                .participationVisibility(attendee.getParticipationVisibility())
                .createdAt(attendee.getCreatedAt())
                .updatedAt(attendee.getUpdatedAt())
                .build();
    }
}
