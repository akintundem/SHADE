package ai.eventplanner.attendee.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceAnalyticsResponse {
    
    private AttendanceSummaryResponse summary;
    private List<CheckInTimeline> checkInTimeline;
    private List<RegistrationTimeline> registrationTimeline;
    private Map<String, Long> attendanceByUserType;
    private List<NoShowAnalysis> noShowAnalysis;
    private String insights;
    private String recommendations;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckInTimeline {
        private LocalDateTime time;
        private Long count;
        private String period;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrationTimeline {
        private LocalDateTime time;
        private Long count;
        private String period;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoShowAnalysis {
        private String reason;
        private Long count;
        private Double percentage;
    }
}
