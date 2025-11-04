package eventplanner.features.attendee.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendInvitationRequest {
    
    @NotEmpty(message = "Recipients list cannot be empty")
    @Email(message = "Valid email addresses are required")
    private List<String> recipients;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    private String customMessage;
    
    private Boolean includeQRCode = false;
    
    private Boolean includeEventDetails = true;
}
