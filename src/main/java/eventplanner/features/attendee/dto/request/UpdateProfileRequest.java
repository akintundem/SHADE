package eventplanner.features.attendee.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    
    private String name;
    
    @Email(message = "Valid email is required")
    private String email;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Valid phone number is required")
    private String phone;
    
    private String dietaryRestrictions;
    
    private String accessibilityNeeds;
    
    private String emergencyContact;
    
    private String emergencyPhone;
    
    private String notes;
}
