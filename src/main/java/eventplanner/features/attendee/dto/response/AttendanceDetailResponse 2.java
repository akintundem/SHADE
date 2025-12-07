package eventplanner.features.attendee.dto.response;

import eventplanner.common.domain.enums.AttendanceStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDetailResponse {
    
    private UUID id;
    private UUID eventId;
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private AttendanceStatus attendanceStatus;
    private LocalDateTime registrationDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String qrCode;
    private Boolean qrCodeUsed;
    private LocalDateTime qrCodeUsedAt;
    private String ticketType;
    private String dietaryRestrictions;
    private String accessibilityNeeds;
    private String emergencyContact;
    private String emergencyPhone;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
