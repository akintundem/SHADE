package eventplanner.features.attendee.service;

import eventplanner.features.attendee.dto.response.AttendanceAnalyticsResponse;
import eventplanner.features.attendee.dto.response.AttendanceSummaryResponse;
import eventplanner.features.attendee.entity.EventAttendance;
import eventplanner.features.attendee.repository.EventAttendanceRepository;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.common.domain.enums.AttendanceStatus;
import eventplanner.common.util.EventValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for attendee analytics and reporting.
 * All methods validate event existence and ensure proper authorization context.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AttendeeAnalyticsService {
    
    private final EventAttendanceRepository attendanceRepository;
    private final EventUserRepository eventUserRepository;
    private final EventValidationUtil eventValidationUtil;
    
    /**
     * Get comprehensive attendance summary for an event.
     * Authorization: Enforced at controller level via RBAC (attendee.analytics.read permission)
     * 
     * @param eventId The event ID
     * @return AttendanceSummaryResponse with statistics
     */
    public AttendanceSummaryResponse getAttendanceSummary(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        
        long totalRegistered = attendances.size();
        long totalConfirmed = attendances.stream()
                .filter(a -> a.getAttendanceStatus() == AttendanceStatus.CONFIRMED)
                .count();
        long totalCheckedIn = attendances.stream()
                .filter(a -> a.getAttendanceStatus() == AttendanceStatus.CHECKED_IN)
                .count();
        long totalAttended = attendances.stream()
                .filter(a -> a.getAttendanceStatus() == AttendanceStatus.ATTENDED)
                .count();
        long totalNoShows = attendances.stream()
                .filter(a -> a.getAttendanceStatus() == AttendanceStatus.NO_SHOW)
                .count();
        long totalCancelled = attendances.stream()
                .filter(a -> a.getAttendanceStatus() == AttendanceStatus.CANCELLED)
                .count();
        
        double checkInRate = totalRegistered > 0 ? (double) totalCheckedIn / totalRegistered * 100 : 0;
        double attendanceRate = totalRegistered > 0 ? (double) totalAttended / totalRegistered * 100 : 0;
        
        Map<String, Long> attendanceByStatus = attendances.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAttendanceStatus().toString(),
                        Collectors.counting()
                ));
        
        Map<String, Long> attendanceByTicketType = attendances.stream()
                .filter(a -> a.getTicketType() != null)
                .collect(Collectors.groupingBy(
                        EventAttendance::getTicketType,
                        Collectors.counting()
                ));
        
        return new AttendanceSummaryResponse(
                totalRegistered,
                totalConfirmed,
                totalCheckedIn,
                totalAttended,
                totalNoShows,
                totalCancelled,
                checkInRate,
                attendanceRate,
                attendanceByStatus,
                attendanceByTicketType,
                "ACTIVE",
                0L, // availableCapacity - would need event capacity info
                0L  // waitlistCount - would need waitlist logic
        );
    }
    
    /**
     * Get comprehensive attendance analytics for an event.
     * Authorization: Enforced at controller level via RBAC (attendee.analytics.read permission)
     * 
     * @param eventId The event ID
     * @return AttendanceAnalyticsResponse with detailed analytics
     */
    public AttendanceAnalyticsResponse getAttendanceAnalytics(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        AttendanceSummaryResponse summary = getAttendanceSummary(eventId);
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        
        // Check-in timeline
        List<AttendanceAnalyticsResponse.CheckInTimeline> checkInTimeline = buildCheckInTimeline(attendances);
        
        // Registration timeline
        List<AttendanceAnalyticsResponse.RegistrationTimeline> registrationTimeline = buildRegistrationTimeline(attendances);
        
        // Attendance by user type
        Map<String, Long> attendanceByUserType = buildAttendanceByUserType(eventId);
        
        // No-show analysis
        List<AttendanceAnalyticsResponse.NoShowAnalysis> noShowAnalysis = buildNoShowAnalysis(attendances);
        
        return new AttendanceAnalyticsResponse(
                summary,
                checkInTimeline,
                registrationTimeline,
                attendanceByUserType,
                noShowAnalysis,
                "Event attendance is tracking well with good check-in rates",
                "Consider sending reminder notifications to improve attendance rates"
        );
    }
    
    /**
     * Get check-in timeline for an event.
     * Authorization: Enforced at controller level via RBAC (attendee.analytics.read permission)
     * 
     * @param eventId The event ID
     * @return List of check-in timeline entries
     */
    public List<AttendanceAnalyticsResponse.CheckInTimeline> getCheckInTimeline(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        return buildCheckInTimeline(attendances);
    }
    
    /**
     * Get attendance breakdown by user type.
     * Authorization: Enforced at controller level via RBAC (attendee.analytics.read permission)
     * 
     * @param eventId The event ID
     * @return Map of user type to count
     */
    public Map<String, Long> getAttendanceByType(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        return buildAttendanceByUserType(eventId);
    }
    
    /**
     * Get no-show analytics for an event.
     * Authorization: Enforced at controller level via RBAC (attendee.analytics.read permission)
     * 
     * @param eventId The event ID
     * @return List of no-show analysis entries
     */
    public List<AttendanceAnalyticsResponse.NoShowAnalysis> getNoShowAnalytics(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        return buildNoShowAnalysis(attendances);
    }
    
    /**
     * Get registration timeline for an event.
     * Authorization: Enforced at controller level via RBAC (attendee.analytics.read permission)
     * 
     * @param eventId The event ID
     * @return List of registration timeline entries
     */
    public List<AttendanceAnalyticsResponse.RegistrationTimeline> getRegistrationTimeline(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        return buildRegistrationTimeline(attendances);
    }
    
    /**
     * Get capacity status for an event.
     * Authorization: Enforced at controller level via RBAC (attendee.analytics.read permission)
     * 
     * @param eventId The event ID
     * @return Map with capacity information
     */
    public Map<String, Object> getCapacityStatus(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        long registered = attendances.size();
        
        Map<String, Object> result = new HashMap<>();
        result.put("registeredCount", registered);
        result.put("capacity", null); // Would need Event entity
        result.put("availableSpots", null);
        result.put("isFull", false);
        result.put("waitlistCount", 0L);
        
        return result;
    }
    
    /**
     * Get waitlist status for an event.
     * Authorization: Enforced at controller level via RBAC (attendee.read permission)
     * 
     * @param eventId The event ID
     * @return Map with waitlist information
     */
    public Map<String, Object> getWaitlistStatus(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        // AttendanceStatus doesn't have WAITLISTED, so we'll use REGISTERED as a proxy
        long waitlisted = attendances.stream()
                .filter(a -> a.getAttendanceStatus() == AttendanceStatus.REGISTERED)
                .count();
        
        Map<String, Object> result = new HashMap<>();
        result.put("waitlistCount", waitlisted);
        result.put("waitlistPositions", new ArrayList<>());
        
        return result;
    }
    
    // Helper methods
    
    private List<AttendanceAnalyticsResponse.CheckInTimeline> buildCheckInTimeline(List<EventAttendance> attendances) {
        Map<LocalDateTime, Long> checkInsByTime = attendances.stream()
                .filter(a -> a.getCheckInTime() != null)
                .collect(Collectors.groupingBy(
                        EventAttendance::getCheckInTime,
                        Collectors.counting()
                ));
        
        return checkInsByTime.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AttendanceAnalyticsResponse.CheckInTimeline(
                        entry.getKey(),
                        entry.getValue(),
                        "hour"
                ))
                .collect(Collectors.toList());
    }
    
    private List<AttendanceAnalyticsResponse.RegistrationTimeline> buildRegistrationTimeline(List<EventAttendance> attendances) {
        Map<LocalDateTime, Long> registrationsByTime = attendances.stream()
                .filter(a -> a.getRegistrationDate() != null)
                .collect(Collectors.groupingBy(
                        EventAttendance::getRegistrationDate,
                        Collectors.counting()
                ));
        
        return registrationsByTime.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AttendanceAnalyticsResponse.RegistrationTimeline(
                        entry.getKey(),
                        entry.getValue(),
                        "day"
                ))
                .collect(Collectors.toList());
    }
    
    private Map<String, Long> buildAttendanceByUserType(UUID eventId) {
        List<EventUser> eventUsers = eventUserRepository.findByEventId(eventId);
        return eventUsers.stream()
                .collect(Collectors.groupingBy(
                        eu -> eu.getUserType() != null ? eu.getUserType().toString() : "UNKNOWN",
                        Collectors.counting()
                ));
    }
    
    private List<AttendanceAnalyticsResponse.NoShowAnalysis> buildNoShowAnalysis(List<EventAttendance> attendances) {
        long totalRegistered = attendances.size();
        long noShows = attendances.stream()
                .filter(a -> a.getAttendanceStatus() == AttendanceStatus.NO_SHOW)
                .count();
        long cancelled = attendances.stream()
                .filter(a -> a.getAttendanceStatus() == AttendanceStatus.CANCELLED)
                .count();
        
        List<AttendanceAnalyticsResponse.NoShowAnalysis> analysis = new ArrayList<>();
        analysis.add(new AttendanceAnalyticsResponse.NoShowAnalysis(
                "No response",
                noShows,
                totalRegistered > 0 ? (double) noShows / totalRegistered * 100 : 0.0
        ));
        analysis.add(new AttendanceAnalyticsResponse.NoShowAnalysis(
                "Last minute cancellation",
                cancelled,
                totalRegistered > 0 ? (double) cancelled / totalRegistered * 100 : 0.0
        ));
        analysis.add(new AttendanceAnalyticsResponse.NoShowAnalysis(
                "Emergency",
                0L,
                0.0
        ));
        
        return analysis;
    }
}

