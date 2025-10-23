package ai.eventplanner.attendee.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {
    
    private Long totalRegistered;
    private Long totalConfirmed;
    private Long totalCheckedIn;
    private Long totalAttended;
    private Long totalNoShows;
    private Long totalCancelled;
    private Double checkInRate;
    private Double attendanceRate;
    private Map<String, Long> attendanceByStatus;
    private Map<String, Long> attendanceByTicketType;
    private String capacityStatus;
    private Long availableCapacity;
    private Long waitlistCount;
}
