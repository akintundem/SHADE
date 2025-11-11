package eventplanner.features.attendee.dto.request;

import eventplanner.features.attendee.entity.AttendeeStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request to update attendee information
 * All fields are optional - only provided fields will be updated
 */
@Data
public class UpdateAttendeeRequest {
    
    private String name;
    
    @Email(message = "Email must be valid")
    private String email;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    private String phone;
    
    private AttendeeStatus rsvpStatus;
    
    // Consent flags
    private Boolean emailConsent;
    private Boolean smsConsent;
    private Boolean dataProcessingConsent;
}
