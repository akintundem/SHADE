package ai.eventplanner.attendee.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequest {
    
    @NotBlank(message = "QR code is required")
    private String qrCode;
    
    private String notes;
}
