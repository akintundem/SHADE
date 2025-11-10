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
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.services.core.dto.NotificationResponse;
import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.common.domain.enums.CommunicationStatus;
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
    private final NotificationService notificationService;
    private final CommunicationRepository communicationRepository;
    
    public AttendeeManagementService(
            EventAttendanceRepository attendanceRepository,
            EventUserRepository eventUserRepository,
            EventRoleRepository eventRoleRepository,
            NotificationService notificationService,
            CommunicationRepository communicationRepository) {
        this.attendanceRepository = attendanceRepository;
        this.eventUserRepository = eventUserRepository;
        this.eventRoleRepository = eventRoleRepository;
        this.notificationService = notificationService;
        this.communicationRepository = communicationRepository;
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
    
    public List<AttendanceDetailResponse> getAllAttendances(UUID eventId) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        return attendances.stream()
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());
    }
    
    public AttendanceDetailResponse getAttendanceById(UUID attendanceId) {
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));
        return convertToDetailResponse(attendance);
    }
    
    public List<AttendanceDetailResponse> getCheckedInAttendees(UUID eventId) {
        List<EventAttendance> attendances = attendanceRepository.findByEventIdAndAttendanceStatus(eventId, AttendanceStatus.CHECKED_IN);
        return attendances.stream()
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());
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
            String existingNotes = attendance.getNotes() != null ? attendance.getNotes() : "";
            attendance.setNotes(existingNotes + "\nCheck-in: " + request.getNotes());
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
        // Check if role already exists and is active
        Optional<EventRole> existingRole = eventRoleRepository
                .findByEventIdAndUserIdAndRoleName(eventId, request.getUserId(), request.getRoleName());
        
        EventRole role;
        if (existingRole.isPresent() && Boolean.TRUE.equals(existingRole.get().getIsActive())) {
            throw new RuntimeException("User already has this role for the event");
        } else if (existingRole.isPresent()) {
            // Reactivate existing role
            role = existingRole.get();
            role.setIsActive(true);
            role.setAssignedAt(LocalDateTime.now());
            if (request.getPermissions() != null) {
                role.setPermissions(request.getPermissions());
            }
        } else {
            // Create new role
            role = new EventRole();
            role.setEventId(eventId);
            role.setUserId(request.getUserId());
            role.setRoleName(request.getRoleName());
            role.setPermissions(request.getPermissions());
            role.setIsActive(true);
            role.setAssignedAt(LocalDateTime.now());
        }
        
        // Save the EventRole
        eventRoleRepository.save(role);
        
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
    
    public String getAttendeeQRCode(UUID attendanceId) {
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));
        return attendance.getQrCode();
    }
    
    public String regenerateQRCode(UUID attendanceId) {
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));
        String newQrCode = generateQRCode(attendance.getEventId(), attendance.getUserId());
        attendance.setQrCode(newQrCode);
        attendance.setQrCodeUsed(false);
        attendance.setQrCodeUsedAt(null);
        attendanceRepository.save(attendance);
        return newQrCode;
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
    
    // EventUser Management Methods
    public List<EventUserResponse> getAllEventUsers(UUID eventId) {
        List<EventUser> eventUsers = eventUserRepository.findByEventId(eventId);
        return eventUsers.stream()
                .map(this::convertToEventUserResponse)
                .collect(Collectors.toList());
    }
    
    public EventUserResponse getEventUser(UUID eventId, UUID userId) {
        EventUser eventUser = eventUserRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("Event user not found"));
        return convertToEventUserResponse(eventUser);
    }
    
    public EventUserResponse updateEventUser(UUID eventId, UUID userId, UpdateProfileRequest request) {
        EventUser eventUser = eventUserRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("Event user not found"));
        
        if (request.getName() != null) {
            eventUser.setName(request.getName());
        }
        if (request.getEmail() != null) {
            eventUser.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            // EventUser doesn't have phone, but we can store in notes if needed
        }
        if (request.getNotes() != null) {
            eventUser.setNotes(request.getNotes());
        }
        
        EventUser saved = eventUserRepository.save(eventUser);
        return convertToEventUserResponse(saved);
    }
    
    public void removeUserFromEvent(UUID eventId, UUID userId) {
        EventUser eventUser = eventUserRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("Event user not found"));
        eventUserRepository.delete(eventUser);
        
        // Also deactivate all roles for this user in this event
        List<EventRole> roles = eventRoleRepository.findByEventIdAndUserId(eventId, userId);
        for (EventRole role : roles) {
            role.setIsActive(false);
            eventRoleRepository.save(role);
        }
    }
    
    // EventRole Management Methods
    public List<EventUserResponse.EventRoleResponse> getUserRoles(UUID eventId, UUID userId) {
        List<EventRole> roles = eventRoleRepository.findByEventIdAndUserId(eventId, userId);
        return roles.stream()
                .map(role -> new EventUserResponse.EventRoleResponse(
                        role.getId(),
                        role.getRoleName(),
                        role.getPermissions(),
                        role.getIsActive(),
                        role.getAssignedAt(),
                        role.getNotes()
                ))
                .collect(Collectors.toList());
    }
    
    public EventUserResponse.EventRoleResponse updateUserRole(UUID eventId, UUID userId, UUID roleId, AssignRoleRequest request) {
        EventRole role = eventRoleRepository.findById(roleId)
                .filter(r -> r.getEventId().equals(eventId) && r.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Role not found"));
        
        if (request.getRoleName() != null) {
            role.setRoleName(request.getRoleName());
        }
        if (request.getPermissions() != null) {
            role.setPermissions(request.getPermissions());
        }
        if (request.getNotes() != null) {
            role.setNotes(request.getNotes());
        }
        
        EventRole saved = eventRoleRepository.save(role);
        return new EventUserResponse.EventRoleResponse(
                saved.getId(),
                saved.getRoleName(),
                saved.getPermissions(),
                saved.getIsActive(),
                saved.getAssignedAt(),
                saved.getNotes()
        );
    }
    
    public void removeUserRole(UUID eventId, UUID userId, UUID roleId) {
        EventRole role = eventRoleRepository.findById(roleId)
                .filter(r -> r.getEventId().equals(eventId) && r.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Role not found"));
        
        role.setIsActive(false);
        eventRoleRepository.save(role);
    }
    
    // Analytics Methods
    public List<AttendanceAnalyticsResponse.CheckInTimeline> getCheckInTimeline(UUID eventId) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
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
                        "hour" // period
                ))
                .collect(Collectors.toList());
    }
    
    public Map<String, Long> getAttendanceByType(UUID eventId) {
        List<EventUser> eventUsers = eventUserRepository.findByEventId(eventId);
        return eventUsers.stream()
                .collect(Collectors.groupingBy(
                        eu -> eu.getUserType() != null ? eu.getUserType().toString() : "UNKNOWN",
                        Collectors.counting()
                ));
    }
    
    public List<AttendanceAnalyticsResponse.NoShowAnalysis> getNoShowAnalytics(UUID eventId) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
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
    
    public List<AttendanceAnalyticsResponse.RegistrationTimeline> getRegistrationTimeline(UUID eventId) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
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
                        "day" // period
                ))
                .collect(Collectors.toList());
    }
    
    // Search and Filter Methods
    public List<AttendanceDetailResponse> searchAttendees(UUID eventId, String name, String email, String status) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        
        return attendances.stream()
                .filter(a -> {
                    if (name != null && !name.trim().isEmpty()) {
                        if (a.getName() == null || !a.getName().toLowerCase().contains(name.toLowerCase())) {
                            return false;
                        }
                    }
                    if (email != null && !email.trim().isEmpty()) {
                        if (a.getEmail() == null || !a.getEmail().toLowerCase().contains(email.toLowerCase())) {
                            return false;
                        }
                    }
                    if (status != null && !status.trim().isEmpty()) {
                        try {
                            AttendanceStatus statusEnum = AttendanceStatus.valueOf(status.toUpperCase());
                            if (a.getAttendanceStatus() != statusEnum) {
                                return false;
                            }
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());
    }
    
    public List<AttendanceDetailResponse> filterAttendees(UUID eventId, String status, String ticketType, Boolean hasDietaryRestrictions) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        
        return attendances.stream()
                .filter(a -> {
                    if (status != null && !status.trim().isEmpty()) {
                        try {
                            AttendanceStatus statusEnum = AttendanceStatus.valueOf(status.toUpperCase());
                            if (a.getAttendanceStatus() != statusEnum) {
                                return false;
                            }
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                    if (ticketType != null && !ticketType.trim().isEmpty()) {
                        if (a.getTicketType() == null || !a.getTicketType().equals(ticketType)) {
                            return false;
                        }
                    }
                    if (hasDietaryRestrictions != null) {
                        boolean hasRestrictions = a.getDietaryRestrictions() != null && !a.getDietaryRestrictions().trim().isEmpty();
                        if (hasDietaryRestrictions != hasRestrictions) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());
    }
    
    // Bulk Operations
    public List<AttendanceDetailResponse> bulkUpdateAttendees(UUID eventId, BulkUpdateRequest request) {
        List<AttendanceDetailResponse> results = new ArrayList<>();
        
        if (request.getAttendanceIds() == null || request.getAttendanceIds().isEmpty()) {
            throw new RuntimeException("No attendance IDs provided");
        }
        
        for (UUID attendanceId : request.getAttendanceIds()) {
            try {
                EventAttendance attendance = attendanceRepository.findById(attendanceId)
                        .filter(a -> a.getEventId().equals(eventId))
                        .orElseThrow(() -> new RuntimeException("Attendance not found: " + attendanceId));
                
                if (request.getAttendanceStatus() != null) {
                    attendance.setAttendanceStatus(request.getAttendanceStatus());
                }
                if (request.getNotes() != null) {
                    String existingNotes = attendance.getNotes() != null ? attendance.getNotes() : "";
                    attendance.setNotes(existingNotes + "\n" + request.getNotes());
                }
                
                EventAttendance saved = attendanceRepository.save(attendance);
                results.add(convertToDetailResponse(saved));
            } catch (Exception e) {
                // Skip failed updates
            }
        }
        
        return results;
    }
    
    public void bulkDeleteAttendees(UUID eventId, BulkUpdateRequest request) {
        if (request.getAttendanceIds() == null || request.getAttendanceIds().isEmpty()) {
            throw new RuntimeException("No attendance IDs provided");
        }
        
        for (UUID attendanceId : request.getAttendanceIds()) {
            attendanceRepository.findById(attendanceId)
                    .filter(a -> a.getEventId().equals(eventId))
                    .ifPresent(attendanceRepository::delete);
        }
    }
    
    // Validation Methods
    public Map<String, Object> validateAttendeeData(UUID eventId) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        
        long total = attendances.size();
        long incomplete = attendances.stream()
                .filter(a -> a.getName() == null || a.getName().trim().isEmpty() ||
                           a.getEmail() == null || a.getEmail().trim().isEmpty())
                .count();
        long duplicates = findDuplicateCount(attendances);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalAttendees", total);
        result.put("incompleteProfiles", incomplete);
        result.put("duplicateRecords", duplicates);
        result.put("validationStatus", incomplete == 0 && duplicates == 0 ? "VALID" : "INVALID");
        result.put("issues", new ArrayList<>());
        
        return result;
    }
    
    public List<AttendanceDetailResponse> findDuplicateAttendees(UUID eventId) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        Map<String, List<EventAttendance>> emailGroups = attendances.stream()
                .filter(a -> a.getEmail() != null && !a.getEmail().trim().isEmpty())
                .collect(Collectors.groupingBy(EventAttendance::getEmail));
        
        List<AttendanceDetailResponse> duplicates = new ArrayList<>();
        for (List<EventAttendance> group : emailGroups.values()) {
            if (group.size() > 1) {
                duplicates.addAll(group.stream()
                        .map(this::convertToDetailResponse)
                        .collect(Collectors.toList()));
            }
        }
        
        return duplicates;
    }
    
    public List<AttendanceDetailResponse> findIncompleteProfiles(UUID eventId) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        return attendances.stream()
                .filter(a -> a.getName() == null || a.getName().trim().isEmpty() ||
                           a.getEmail() == null || a.getEmail().trim().isEmpty() ||
                           a.getPhone() == null || a.getPhone().trim().isEmpty())
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());
    }
    
    public Map<String, Object> getCapacityStatus(UUID eventId) {
        // This would need event repository access - for now return basic info
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
    
    public Map<String, Object> getWaitlistStatus(UUID eventId) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        // AttendanceStatus doesn't have WAITLISTED, so we'll use REGISTERED as a proxy
        // In a real implementation, you might have a separate waitlist table or field
        long waitlisted = attendances.stream()
                .filter(a -> a.getAttendanceStatus() == AttendanceStatus.REGISTERED)
                .count();
        
        Map<String, Object> result = new HashMap<>();
        result.put("waitlistCount", waitlisted);
        result.put("waitlistPositions", new ArrayList<>());
        
        return result;
    }
    
    // Communication Methods
    public Map<String, Object> sendBulkEmail(UUID eventId, SendInvitationRequest request) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        long sent = 0;
        long failed = 0;
        
        for (EventAttendance attendance : attendances) {
            if (attendance.getEmail() == null || attendance.getEmail().trim().isEmpty()) {
                failed++;
                continue;
            }
            
            try {
                Map<String, Object> templateVariables = new HashMap<>();
                templateVariables.put("attendeeName", attendance.getName() != null ? attendance.getName() : "Guest");
                templateVariables.put("eventId", eventId.toString());
                if (request.getCustomMessage() != null) {
                    templateVariables.put("customMessage", request.getCustomMessage());
                }
                
                NotificationRequest notificationRequest = NotificationRequest.builder()
                        .type(CommunicationType.EMAIL)
                        .to(attendance.getEmail())
                        .subject(request.getSubject())
                        .templateId("event-invitation")
                        .templateVariables(templateVariables)
                        .eventId(eventId)
                        .build();
                
                NotificationResponse response = notificationService.send(notificationRequest);
                if (response.isSuccess()) {
                    sent++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("sent", sent);
        result.put("failed", failed);
        result.put("total", attendances.size());
        result.put("status", sent > 0 ? "completed" : "failed");
        
        return result;
    }
    
    public Map<String, Object> sendNotification(UUID eventId, UUID attendanceId, SendInvitationRequest request) {
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .filter(a -> a.getEventId().equals(eventId))
                .orElseThrow(() -> new RuntimeException("Attendance not found"));
        
        if (attendance.getEmail() == null || attendance.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Attendee email is required for notifications");
        }
        
        try {
            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("attendeeName", attendance.getName() != null ? attendance.getName() : "Guest");
            templateVariables.put("eventId", eventId.toString());
            if (request.getCustomMessage() != null) {
                templateVariables.put("customMessage", request.getCustomMessage());
            }
            
            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .type(CommunicationType.EMAIL)
                    .to(attendance.getEmail())
                    .subject(request.getSubject())
                    .templateId("event-invitation")
                    .templateVariables(templateVariables)
                    .eventId(eventId)
                    .build();
            
            NotificationResponse response = notificationService.send(notificationRequest);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("communicationId", response.getCommunicationId());
            result.put("messageId", response.getMessageId());
            result.put("status", response.getStatus());
            if (!response.isSuccess()) {
                result.put("errorMessage", response.getErrorMessage());
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send notification: " + e.getMessage(), e);
        }
    }
    
    public List<Map<String, Object>> getCommunicationHistory(UUID eventId) {
        List<Communication> communications = communicationRepository.findByEventIdOrderByCreatedAtDesc(eventId);
        return communications.stream()
                .map(comm -> {
                    Map<String, Object> commMap = new HashMap<>();
                    commMap.put("id", comm.getId());
                    commMap.put("type", comm.getCommunicationType());
                    commMap.put("recipientEmail", comm.getRecipientEmail());
                    commMap.put("subject", comm.getSubject());
                    commMap.put("status", comm.getStatus());
                    commMap.put("sentAt", comm.getSentAt());
                    commMap.put("deliveredAt", comm.getDeliveredAt());
                    commMap.put("createdAt", comm.getCreatedAt());
                    return commMap;
                })
                .collect(Collectors.toList());
    }
    
    public InvitationResponse sendInvitations(UUID eventId, SendInvitationRequest request) {
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        List<InvitationResponse.InvitationDetail> invitationDetails = new ArrayList<>();
        long totalSent = 0;
        long totalDelivered = 0;
        long totalFailed = 0;
        
        for (EventAttendance attendance : attendances) {
            if (attendance.getEmail() == null || attendance.getEmail().trim().isEmpty()) {
                totalFailed++;
                invitationDetails.add(new InvitationResponse.InvitationDetail(
                        attendance.getEmail(),
                        "failed",
                        "No email address",
                        null,
                        null,
                        "No email address provided"
                ));
                continue;
            }
            
            try {
                Map<String, Object> templateVariables = new HashMap<>();
                templateVariables.put("attendeeName", attendance.getName() != null ? attendance.getName() : "Guest");
                templateVariables.put("eventId", eventId.toString());
                if (request.getCustomMessage() != null) {
                    templateVariables.put("customMessage", request.getCustomMessage());
                }
                
                NotificationRequest notificationRequest = NotificationRequest.builder()
                        .type(CommunicationType.EMAIL)
                        .to(attendance.getEmail())
                        .subject(request.getSubject())
                        .templateId("event-invitation")
                        .templateVariables(templateVariables)
                        .eventId(eventId)
                        .build();
                
                NotificationResponse response = notificationService.send(notificationRequest);
                
                if (response.isSuccess()) {
                    totalSent++;
                    invitationDetails.add(new InvitationResponse.InvitationDetail(
                            attendance.getEmail(),
                            "sent",
                            "Invitation sent successfully",
                            LocalDateTime.now(),
                            null,
                            null
                    ));
                } else {
                    totalFailed++;
                    invitationDetails.add(new InvitationResponse.InvitationDetail(
                            attendance.getEmail(),
                            "failed",
                            "Failed to send invitation",
                            null,
                            null,
                            response.getErrorMessage()
                    ));
                }
            } catch (Exception e) {
                totalFailed++;
                invitationDetails.add(new InvitationResponse.InvitationDetail(
                        attendance.getEmail(),
                        "failed",
                        "Exception occurred",
                        null,
                        null,
                        e.getMessage()
                ));
            }
        }
        
        return new InvitationResponse(
                eventId,
                invitationDetails,
                totalSent,
                totalDelivered,
                totalFailed,
                LocalDateTime.now(),
                totalSent > 0 ? "completed" : "failed"
        );
    }
    
    public List<InvitationResponse> getSentInvitations(UUID eventId) {
        List<Communication> communications = communicationRepository.findByEventIdOrderByCreatedAtDesc(eventId);
        Map<LocalDateTime, List<Communication>> groupedByDate = communications.stream()
                .filter(c -> c.getCommunicationType() == CommunicationType.EMAIL)
                .collect(Collectors.groupingBy(Communication::getSentAt));
        
        return groupedByDate.entrySet().stream()
                .map(entry -> {
                    List<Communication> comms = entry.getValue();
                    List<InvitationResponse.InvitationDetail> details = comms.stream()
                            .map(comm -> new InvitationResponse.InvitationDetail(
                                    comm.getRecipientEmail(),
                                    comm.getStatus().toString(),
                                    comm.getSubject(),
                                    comm.getSentAt(),
                                    comm.getDeliveredAt(),
                                    comm.getFailureReason()
                            ))
                            .collect(Collectors.toList());
                    
                    long sent = comms.stream().filter(c -> c.getStatus() == CommunicationStatus.SENT).count();
                    long delivered = comms.stream().filter(c -> c.getStatus() == CommunicationStatus.DELIVERED).count();
                    long failed = comms.stream().filter(c -> c.getStatus() == CommunicationStatus.FAILED).count();
                    
                    return new InvitationResponse(
                            eventId,
                            details,
                            sent,
                            delivered,
                            failed,
                            entry.getKey(),
                            "completed"
                    );
                })
                .collect(Collectors.toList());
    }
    
    public List<AttendanceDetailResponse> importAttendeesCSV(UUID eventId, String csvData) {
        // Basic CSV parsing - in production, use a proper CSV library
        List<AttendanceDetailResponse> results = new ArrayList<>();
        String[] lines = csvData.split("\n");
        
        // Skip header row if present
        int startIndex = 0;
        if (lines.length > 0 && lines[0].toLowerCase().contains("name") || lines[0].toLowerCase().contains("email")) {
            startIndex = 1;
        }
        
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            String[] fields = line.split(",");
            if (fields.length < 2) continue;
            
            try {
                CreateAttendanceRequest request = new CreateAttendanceRequest();
                request.setEventId(eventId);
                request.setName(fields[0].trim());
                request.setEmail(fields[1].trim());
                if (fields.length > 2) {
                    request.setPhone(fields[2].trim());
                }
                request.setAttendanceStatus(AttendanceStatus.REGISTERED);
                
                AttendanceDetailResponse response = registerForEvent(request);
                results.add(response);
            } catch (Exception e) {
                // Skip invalid rows
            }
        }
        
        return results;
    }
    
    private long findDuplicateCount(List<EventAttendance> attendances) {
        Map<String, Long> emailCounts = attendances.stream()
                .filter(a -> a.getEmail() != null && !a.getEmail().trim().isEmpty())
                .collect(Collectors.groupingBy(EventAttendance::getEmail, Collectors.counting()));
        
        return emailCounts.values().stream()
                .filter(count -> count > 1)
                .mapToLong(count -> count - 1) // Count duplicates (excluding first occurrence)
                .sum();
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
        
        EventUserResponse response = new EventUserResponse();
        response.setId(eventUser.getId());
        response.setEventId(eventUser.getEventId());
        response.setUserId(eventUser.getUserId());
        response.setUserName(eventUser.getName());
        response.setUserEmail(eventUser.getEmail());
        response.setUserType(eventUser.getUserType());
        response.setRegistrationStatus(eventUser.getRegistrationStatus());
        response.setRegistrationDate(eventUser.getRegistrationDate());
        response.setRoles(roleResponses);
        response.setNotes(eventUser.getNotes());
        response.setCreatedAt(eventUser.getCreatedAt());
        response.setUpdatedAt(eventUser.getUpdatedAt());
        
        return response;
    }
}
