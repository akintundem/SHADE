package eventplanner.features.attendee.dto.request;

import eventplanner.features.attendee.entity.Attendee;
import jakarta.validation.constraints.Email;
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
    
    private Attendee.Status rsvpStatus;
}
