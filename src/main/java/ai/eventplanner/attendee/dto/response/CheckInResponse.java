package ai.eventplanner.attendee.dto.response;

import ai.eventplanner.common.domain.enums.AttendanceStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponse {
    
    private UUID attendanceId;
    private UUID eventId;
    private String attendeeName;
    private String attendeeEmail;
    private AttendanceStatus previousStatus;
    private AttendanceStatus currentStatus;
    private LocalDateTime checkInTime;
    private String qrCode;
    private Boolean qrCodeUsed;
    private String message;
    private String notes;
}
