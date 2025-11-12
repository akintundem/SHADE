package eventplanner.features.attendee.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequest {
    
    // QR code is optional - can be used for QR-based check-in
    private String qrCode;
    
    // Manual check-in fields
    private String checkInMethod;  // e.g., "MANUAL", "QR_CODE", "MOBILE_APP"
    
    private String checkInLocation;  // e.g., "Main Entrance", "Gate 1"
    
    private String notes;
}
