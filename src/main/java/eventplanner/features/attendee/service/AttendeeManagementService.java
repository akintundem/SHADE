package eventplanner.features.attendee.service;

import eventplanner.features.attendee.dto.request.*;
import eventplanner.features.attendee.dto.response.*;
import eventplanner.features.attendee.entity.EventAttendance;
import eventplanner.features.attendee.repository.EventAttendanceRepository;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.security.authorization.domain.entity.EventRole;
import eventplanner.security.authorization.domain.repository.EventRoleRepository;
import eventplanner.common.domain.enums.AttendanceStatus;
import eventplanner.common.domain.enums.EventUserType;
import eventplanner.common.domain.enums.RegistrationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendeeManagementService {
    
    private final EventAttendanceRepository attendanceRepository;
    private final EventUserRepository eventUserRepository;
    private final EventRoleRepository eventRoleRepository;
    
    public AttendeeManagementService(
            EventAttendanceRepository attendanceRepository,
            EventUserRepository eventUserRepository,
            EventRoleRepository eventRoleRepository) {
        this.attendanceRepository = attendanceRepository;
        this.eventUserRepository = eventUserRepository;
        this.eventRoleRepository = eventRoleRepository;
    }
    
    // Core Attendance Management
    public AttendanceDetailResponse registerForEvent(CreateAttendanceRequest request) {
        EventAttendance attendance = new EventAttendance();
        attendance.setEventId(request.getEventId());
        attendance.setUserId(request.getUserId());
        attendance.setName(request.getName());
        attendance.setEmail(request.getEmail());
        attendance.setPhone(request.getPhone());
        attendance.setAttendanceStatus(request.getAttendanceStatus());
        attendance.setRegistrationDate(LocalDateTime.now());
        attendance.setTicketType(request.getTicketType());
        attendance.setDietaryRestrictions(request.getDietaryRestrictions());
        attendance.setAccessibilityNeeds(request.getAccessibilityNeeds());
        attendance.setEmergencyContact(request.getEmergencyContact());
        attendance.setEmergencyPhone(request.getEmergencyPhone());
        attendance.setNotes(request.getNotes());
        attendance.setQrCode(generateQRCode(request.getEventId(), request.getUserId()));
        
        EventAttendance saved = attendanceRepository.save(attendance);
        return convertToDetailResponse(saved);
    }
    
    public List<AttendanceDetailResponse> bulkRegister(BulkAttendanceRequest request) {
        List<EventAttendance> attendances = request.getAttendees().stream()
                .map(attendee -> {
                    EventAttendance attendance = new EventAttendance();
                    attendance.setEventId(request.getEventId());
                    attendance.setUserId(attendee.getUserId());
                    attendance.setName(attendee.getName());
                    attendance.setEmail(attendee.getEmail());
                    attendance.setPhone(attendee.getPhone());
                    attendance.setAttendanceStatus(attendee.getAttendanceStatus());
                    attendance.setRegistrationDate(LocalDateTime.now());
                    attendance.setTicketType(attendee.getTicketType());
                    attendance.setDietaryRestrictions(attendee.getDietaryRestrictions());
                    attendance.setAccessibilityNeeds(attendee.getAccessibilityNeeds());
                    attendance.setEmergencyContact(attendee.getEmergencyContact());
                    attendance.setEmergencyPhone(attendee.getEmergencyPhone());
                    attendance.setNotes(attendee.getNotes());
                    attendance.setQrCode(generateQRCode(request.getEventId(), attendee.getUserId()));
                    return attendance;
                })
                .collect(Collectors.toList());
        
        List<EventAttendance> saved = attendanceRepository.saveAll(attendances);
        return saved.stream().map(this::convertToDetailResponse).collect(Collectors.toList());
    }
    
    public AttendanceDetailResponse updateAttendance(UUID attendanceId, UpdateAttendanceRequest request) {
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));
        
        if (request.getName() != null) attendance.setName(request.getName());
        if (request.getEmail() != null) attendance.setEmail(request.getEmail());
        if (request.getPhone() != null) attendance.setPhone(request.getPhone());
        if (request.getAttendanceStatus() != null) attendance.setAttendanceStatus(request.getAttendanceStatus());
        if (request.getTicketType() != null) attendance.setTicketType(request.getTicketType());
        if (request.getDietaryRestrictions() != null) attendance.setDietaryRestrictions(request.getDietaryRestrictions());
        if (request.getAccessibilityNeeds() != null) attendance.setAccessibilityNeeds(request.getAccessibilityNeeds());
        if (request.getEmergencyContact() != null) attendance.setEmergencyContact(request.getEmergencyContact());
        if (request.getEmergencyPhone() != null) attendance.setEmergencyPhone(request.getEmergencyPhone());
        if (request.getNotes() != null) attendance.setNotes(request.getNotes());
        
        EventAttendance saved = attendanceRepository.save(attendance);
        return convertToDetailResponse(saved);
    }
    
    public void cancelAttendance(UUID attendanceId) {
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));
        
        attendance.setAttendanceStatus(AttendanceStatus.CANCELLED);
        attendanceRepository.save(attendance);
    }
    
    // Check-in/Check-out Management
    public CheckInResponse checkInAttendee(UUID attendanceId, CheckInRequest request) {
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));
        
        AttendanceStatus previousStatus = attendance.getAttendanceStatus();
        attendance.setAttendanceStatus(AttendanceStatus.CHECKED_IN);
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setQrCodeUsed(true);
        attendance.setQrCodeUsedAt(LocalDateTime.now());
        if (request.getNotes() != null) {
            attendance.setNotes(attendance.getNotes() + "\nCheck-in: " + request.getNotes());
        }
        
        EventAttendance saved = attendanceRepository.save(attendance);
        
        return new CheckInResponse(
                saved.getId(),
                saved.getEventId(),
                saved.getName(),
                saved.getEmail(),
                previousStatus,
                saved.getAttendanceStatus(),
                saved.getCheckInTime(),
                saved.getQrCode(),
                saved.getQrCodeUsed(),
                "Successfully checked in",
                request.getNotes()
        );
    }
    
    public CheckInResponse checkOutAttendee(UUID attendanceId) {
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));
        
        AttendanceStatus previousStatus = attendance.getAttendanceStatus();
        attendance.setAttendanceStatus(AttendanceStatus.ATTENDED);
        attendance.setCheckOutTime(LocalDateTime.now());
        
        EventAttendance saved = attendanceRepository.save(attendance);
        
        return new CheckInResponse(
                saved.getId(),
                saved.getEventId(),
                saved.getName(),
                saved.getEmail(),
                previousStatus,
                saved.getAttendanceStatus(),
                saved.getCheckOutTime(),
                saved.getQrCode(),
                saved.getQrCodeUsed(),
                "Successfully checked out",
                null
        );
    }
    
    public CheckInResponse scanQRCode(UUID eventId, String qrCode) {
        EventAttendance attendance = attendanceRepository.findByEventIdAndQrCode(eventId, qrCode)
                .orElseThrow(() -> new RuntimeException("Invalid QR code"));
        
        if (attendance.getQrCodeUsed()) {
            throw new RuntimeException("QR code already used");
        }
        
        return checkInAttendee(attendance.getId(), new CheckInRequest(qrCode, "QR code scan"));
    }
    
    // User-Event Relationship Management
    public EventUserResponse addUserToEvent(UUID eventId, UUID userId, EventUserType userType) {
        EventUser eventUser = new EventUser();
        eventUser.setEventId(eventId);
        eventUser.setUserId(userId);
        eventUser.setUserType(userType);
        eventUser.setRegistrationStatus(RegistrationStatus.CONFIRMED);
        eventUser.setRegistrationDate(LocalDateTime.now());
        
        EventUser saved = eventUserRepository.save(eventUser);
        return convertToEventUserResponse(saved);
    }
    
    public EventUserResponse assignRole(UUID eventId, AssignRoleRequest request) {
        EventRole role = new EventRole();
        role.setEventId(eventId);
        role.setUserId(request.getUserId());
        role.setRoleName(request.getRoleName());
        role.setPermissions(request.getPermissions());
        role.setIsActive(true);
        role.setAssignedAt(LocalDateTime.now());
        
        
        // Update EventUser if needed
        EventUser eventUser = eventUserRepository.findByEventIdAndUserId(eventId, request.getUserId())
                .orElse(null);
        if (eventUser == null) {
            eventUser = new EventUser();
            eventUser.setEventId(eventId);
            eventUser.setUserId(request.getUserId());
            eventUser.setUserType(EventUserType.STAFF);
            eventUser.setRegistrationStatus(RegistrationStatus.CONFIRMED);
            eventUser.setRegistrationDate(LocalDateTime.now());
            eventUserRepository.save(eventUser);
        }
        
        return convertToEventUserResponse(eventUser);
    }
    
    // Analytics and Reporting
    public AttendanceSummaryResponse getAttendanceSummary(UUID eventId) {
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
    
    public AttendanceAnalyticsResponse getAttendanceAnalytics(UUID eventId) {
        AttendanceSummaryResponse summary = getAttendanceSummary(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        
        // Check-in timeline
        List<AttendanceAnalyticsResponse.CheckInTimeline> checkInTimeline = attendances.stream()
                .filter(a -> a.getCheckInTime() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCheckInTime().toLocalDate().toString(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> new AttendanceAnalyticsResponse.CheckInTimeline(
                        LocalDateTime.parse(entry.getKey() + "T00:00:00"),
                        entry.getValue(),
                        "daily"
                ))
                .collect(Collectors.toList());
        
        // Registration timeline
        List<AttendanceAnalyticsResponse.RegistrationTimeline> registrationTimeline = attendances.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getRegistrationDate().toLocalDate().toString(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> new AttendanceAnalyticsResponse.RegistrationTimeline(
                        LocalDateTime.parse(entry.getKey() + "T00:00:00"),
                        entry.getValue(),
                        "daily"
                ))
                .collect(Collectors.toList());
        
        // Attendance by user type
        Map<String, Long> attendanceByUserType = new HashMap<>();
        List<EventUser> eventUsers = eventUserRepository.findByEventId(eventId);
        for (EventUser user : eventUsers) {
            String userType = user.getUserType().toString();
            attendanceByUserType.merge(userType, 1L, Long::sum);
        }
        
        // No-show analysis
        List<AttendanceAnalyticsResponse.NoShowAnalysis> noShowAnalysis = Arrays.asList(
                new AttendanceAnalyticsResponse.NoShowAnalysis("No response", 0L, 0.0),
                new AttendanceAnalyticsResponse.NoShowAnalysis("Last minute cancellation", 0L, 0.0),
                new AttendanceAnalyticsResponse.NoShowAnalysis("Emergency", 0L, 0.0)
        );
        
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
    
    // Export functionality
    public ExportResponse exportAttendees(UUID eventId, String format) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        
        String exportId = UUID.randomUUID().toString();
        String fileName = "attendees_" + eventId + "_" + System.currentTimeMillis() + "." + format.toLowerCase();
        
        return new ExportResponse(
                exportId,
                format,
                "COMPLETED",
                "/api/v1/exports/" + exportId + "/download",
                (long) attendances.size(),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7),
                Arrays.asList("name", "email", "phone", "attendanceStatus", "checkInTime", "ticketType"),
                fileName
        );
    }
    
    // Utility methods
    private String generateQRCode(UUID eventId, UUID userId) {
        return "QR_" + eventId.toString().substring(0, 8) + "_" + userId.toString().substring(0, 8) + "_" + System.currentTimeMillis();
    }
    
    private AttendanceDetailResponse convertToDetailResponse(EventAttendance attendance) {
        return new AttendanceDetailResponse(
                attendance.getId(),
                attendance.getEventId(),
                attendance.getUserId(),
                attendance.getName(),
                attendance.getEmail(),
                attendance.getPhone(),
                attendance.getAttendanceStatus(),
                attendance.getRegistrationDate(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                attendance.getQrCode(),
                attendance.getQrCodeUsed(),
                attendance.getQrCodeUsedAt(),
                attendance.getTicketType(),
                attendance.getDietaryRestrictions(),
                attendance.getAccessibilityNeeds(),
                attendance.getEmergencyContact(),
                attendance.getEmergencyPhone(),
                attendance.getNotes(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }
    
    private EventUserResponse convertToEventUserResponse(EventUser eventUser) {
        List<EventRole> roles = eventRoleRepository.findByEventIdAndUserId(eventUser.getEventId(), eventUser.getUserId());
        List<EventUserResponse.EventRoleResponse> roleResponses = roles.stream()
                .map(role -> new EventUserResponse.EventRoleResponse(
                        role.getId(),
                        role.getRoleName(),
                        role.getPermissions(),
                        role.getIsActive(),
                        role.getAssignedAt(),
                        role.getNotes()
                ))
                .collect(Collectors.toList());
        
        return new EventUserResponse(
                eventUser.getId(),
                eventUser.getEventId(),
                eventUser.getUserId(),
                eventUser.getName(),
                eventUser.getEmail(),
                eventUser.getUserType(),
                eventUser.getRegistrationStatus(),
                eventUser.getRegistrationDate(),
                roleResponses,
                eventUser.getNotes(),
                eventUser.getCreatedAt(),
                eventUser.getUpdatedAt()
        );
    }
}
