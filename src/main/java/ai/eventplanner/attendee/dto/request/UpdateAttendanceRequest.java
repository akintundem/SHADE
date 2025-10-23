package ai.eventplanner.attendee.dto.request;

import ai.eventplanner.common.domain.enums.AttendanceStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAttendanceRequest {
    
    private String name;
    
    @Email(message = "Valid email is required")
    private String email;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Valid phone number is required")
    private String phone;
    
    private AttendanceStatus attendanceStatus;
    
    private String ticketType;
    
    private String dietaryRestrictions;
    
    private String accessibilityNeeds;
    
    private String emergencyContact;
    
    private String emergencyPhone;
    
    private String notes;
}
