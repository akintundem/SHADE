package eventplanner.features.attendee.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eventplanner.features.attendee.entity.AttendeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sanitized attendee response that doesn't leak internal JPA fields
 * Includes consent flags and privacy controls
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendeeResponse {
    
    private UUID id;
    private UUID eventId;
    private String name;
    
    // Contact information - only included if consent given
    private String email;
    private String phone;
    
    // Status information
    private AttendeeStatus rsvpStatus;
    private LocalDateTime checkedInAt;
    
    // Consent flags (for privacy compliance)
    private Boolean emailConsent;
    private Boolean smsConsent;
    private Boolean dataProcessingConsent;
    
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
