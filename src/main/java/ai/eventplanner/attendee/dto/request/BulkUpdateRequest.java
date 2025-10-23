package ai.eventplanner.attendee.dto.request;

import ai.eventplanner.common.domain.enums.AttendanceStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateRequest {
    
    @NotNull(message = "Event ID is required")
    private UUID eventId;
    
    @NotEmpty(message = "Attendance IDs list cannot be empty")
    private List<UUID> attendanceIds;
    
    private AttendanceStatus attendanceStatus;
    
    private String notes;
    
    private String updateReason;
}
