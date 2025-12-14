package eventplanner.features.attendee.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eventplanner.features.attendee.entity.Attendee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sanitized attendee response that doesn't leak internal JPA fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendeeResponse {
    
    private UUID id;
    private UUID eventId;
    private UUID userId; // User account ID if attendee is linked to a user in the platform
    private String name;
    private String email;
    
    // Status information
    private Attendee.Status rsvpStatus;
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
