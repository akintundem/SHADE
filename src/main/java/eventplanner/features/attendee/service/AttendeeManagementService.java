package eventplanner.features.attendee.service;

import eventplanner.common.qrcode.model.QRCodeGenerationResult;
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
import eventplanner.common.util.EventValidationUtil;
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
    private final EventValidationUtil eventValidationUtil;
    
    // Specialized services
    private final AttendeeCommunicationService communicationService;
    private final AttendeeAnalyticsService analyticsService;
    private final AttendeeQRCodeService qrCodeService;
    private final AttendeeExportService exportService;
    private final AttendeeSearchService searchService;
    
    public AttendeeManagementService(
            EventAttendanceRepository attendanceRepository,
            EventUserRepository eventUserRepository,
            EventRoleRepository eventRoleRepository,
            EventValidationUtil eventValidationUtil,
            AttendeeCommunicationService communicationService,
            AttendeeAnalyticsService analyticsService,
            AttendeeQRCodeService qrCodeService,
            AttendeeExportService exportService,
            AttendeeSearchService searchService) {
        this.attendanceRepository = attendanceRepository;
        this.eventUserRepository = eventUserRepository;
        this.eventRoleRepository = eventRoleRepository;
        this.eventValidationUtil = eventValidationUtil;
        this.communicationService = communicationService;
        this.analyticsService = analyticsService;
        this.qrCodeService = qrCodeService;
        this.exportService = exportService;
        this.searchService = searchService;
    }
    
    
    // Core Attendance Management
    public AttendanceDetailResponse registerForEvent(CreateAttendanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getEventId() == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        
        eventValidationUtil.validateEventExists(request.getEventId());
        
        // Check if user is already registered
        if (request.getUserId() != null) {
            Optional<EventAttendance> existing = attendanceRepository.findByEventIdAndUserId(
                    request.getEventId(), request.getUserId());
            if (existing.isPresent() && existing.get().getAttendanceStatus() != AttendanceStatus.CANCELLED) {
                throw new RuntimeException("User is already registered for this event");
            }
        }
        
        EventAttendance attendance = new EventAttendance();
        attendance.setEventId(request.getEventId());
        attendance.setUserId(request.getUserId());
        attendance.setName(request.getName());
        attendance.setEmail(request.getEmail());
        attendance.setPhone(request.getPhone());
        attendance.setAttendanceStatus(request.getAttendanceStatus() != null 
                ? request.getAttendanceStatus() 
                : AttendanceStatus.REGISTERED);
        attendance.setRegistrationDate(LocalDateTime.now());
        attendance.setTicketType(request.getTicketType());
        attendance.setDietaryRestrictions(request.getDietaryRestrictions());
        attendance.setAccessibilityNeeds(request.getAccessibilityNeeds());
        attendance.setEmergencyContact(request.getEmergencyContact());
        attendance.setEmergencyPhone(request.getEmergencyPhone());
        attendance.setNotes(request.getNotes() != null ? request.getNotes() : "");
        attendance.setQrCode(qrCodeService.generateQRCode(request.getEventId(), request.getUserId()));
        
        EventAttendance saved = attendanceRepository.save(attendance);
        return convertToDetailResponse(saved);
    }
    
    public List<AttendanceDetailResponse> bulkRegister(BulkAttendanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getEventId() == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (request.getAttendees() == null || request.getAttendees().isEmpty()) {
            throw new IllegalArgumentException("Attendees list cannot be empty");
        }
        eventValidationUtil.validateEventExists(request.getEventId());

        List<EventAttendance> attendances = request.getAttendees().stream()
                .map(attendee -> {
                    EventAttendance attendance = new EventAttendance();
                    attendance.setEventId(request.getEventId());
                    attendance.setUserId(attendee.getUserId());
                    attendance.setName(attendee.getName());
                    attendance.setEmail(attendee.getEmail());
                    attendance.setPhone(attendee.getPhone());
                    AttendanceStatus status = attendee.getAttendanceStatus() != null
                            ? attendee.getAttendanceStatus()
                            : AttendanceStatus.REGISTERED;
                    attendance.setAttendanceStatus(status);
                    attendance.setRegistrationDate(LocalDateTime.now());
                    attendance.setTicketType(attendee.getTicketType());
                    attendance.setDietaryRestrictions(attendee.getDietaryRestrictions());
                    attendance.setAccessibilityNeeds(attendee.getAccessibilityNeeds());
                    attendance.setEmergencyContact(attendee.getEmergencyContact());
                    attendance.setEmergencyPhone(attendee.getEmergencyPhone());
                    attendance.setNotes(attendee.getNotes() != null ? attendee.getNotes() : "");
                    attendance.setQrCode(qrCodeService.generateQRCode(request.getEventId(), attendee.getUserId()));
                    return attendance;
                })
                .collect(Collectors.toList());
        
        List<EventAttendance> saved = attendanceRepository.saveAll(attendances);
        return saved.stream().map(this::convertToDetailResponse).collect(Collectors.toList());
    }
    
    public AttendanceDetailResponse updateAttendance(UUID attendanceId, UpdateAttendanceRequest request) {
        if (attendanceId == null) {
            throw new IllegalArgumentException("Attendance ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found: " + attendanceId));
        
        eventValidationUtil.validateEventExists(attendance.getEventId());
        // Note: Event-level authorization is enforced by RBAC at controller level
        
        if (request.getName() != null) attendance.setName(request.getName());
        if (request.getEmail() != null) attendance.setEmail(request.getEmail());
        if (request.getPhone() != null) attendance.setPhone(request.getPhone());
        if (request.getAttendanceStatus() != null) attendance.setAttendanceStatus(request.getAttendanceStatus());
        if (request.getTicketType() != null) attendance.setTicketType(request.getTicketType());
        if (request.getDietaryRestrictions() != null) attendance.setDietaryRestrictions(request.getDietaryRestrictions());
        if (request.getAccessibilityNeeds() != null) attendance.setAccessibilityNeeds(request.getAccessibilityNeeds());
        if (request.getEmergencyContact() != null) attendance.setEmergencyContact(request.getEmergencyContact());
        if (request.getEmergencyPhone() != null) attendance.setEmergencyPhone(request.getEmergencyPhone());
        if (request.getNotes() != null) {
            String existingNotes = attendance.getNotes() != null ? attendance.getNotes() : "";
            attendance.setNotes(existingNotes + (existingNotes.isEmpty() ? "" : "\n") + request.getNotes());
        }
        
        EventAttendance saved = attendanceRepository.save(attendance);
        return convertToDetailResponse(saved);
    }
    
    public void cancelAttendance(UUID attendanceId) {
        if (attendanceId == null) {
            throw new IllegalArgumentException("Attendance ID cannot be null");
        }
        
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found: " + attendanceId));
        
        eventValidationUtil.validateEventExists(attendance.getEventId());
        
        attendance.setAttendanceStatus(AttendanceStatus.CANCELLED);
        attendanceRepository.save(attendance);
    }
    
    public List<AttendanceDetailResponse> getAllAttendances(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        return attendances.stream()
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());
    }
    
    public AttendanceDetailResponse getAttendanceById(UUID attendanceId) {
        if (attendanceId == null) {
            throw new IllegalArgumentException("Attendance ID cannot be null");
        }
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found: " + attendanceId));
        eventValidationUtil.validateEventExists(attendance.getEventId());
        return convertToDetailResponse(attendance);
    }
    
    public List<AttendanceDetailResponse> getCheckedInAttendees(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventIdAndAttendanceStatus(eventId, AttendanceStatus.CHECKED_IN);
        return attendances.stream()
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());
    }
    
    // Check-in/Check-out Management
    public CheckInResponse checkInAttendee(UUID attendanceId, CheckInRequest request) {
        if (attendanceId == null) {
            throw new IllegalArgumentException("Attendance ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found: " + attendanceId));
        
        eventValidationUtil.validateEventExists(attendance.getEventId());
        
        // Prevent duplicate check-ins
        if (attendance.getAttendanceStatus() == AttendanceStatus.CHECKED_IN) {
            throw new RuntimeException("Attendee is already checked in");
        }
        
        AttendanceStatus previousStatus = attendance.getAttendanceStatus();
        attendance.setAttendanceStatus(AttendanceStatus.CHECKED_IN);
        attendance.setCheckInTime(LocalDateTime.now());
        
        // Handle QR code check-in vs manual check-in
        if (request.getQrCode() != null && !request.getQrCode().trim().isEmpty()) {
            // QR code-based check-in
            attendance.setQrCodeUsed(true);
            attendance.setQrCodeUsedAt(LocalDateTime.now());
        } else {
            // Manual check-in - QR code not required
            // Only mark as QR code used if it was actually scanned
            if (attendance.getQrCode() != null && attendance.getQrCodeUsed()) {
                // QR code was already used, keep the status
            } else {
                attendance.setQrCodeUsed(false);
            }
        }
        
        // Build check-in note with method and location if provided
        String existingNotes = attendance.getNotes() != null ? attendance.getNotes() : "";
        StringBuilder checkInNoteBuilder = new StringBuilder();
        
        if (request.getCheckInMethod() != null || request.getCheckInLocation() != null) {
            checkInNoteBuilder.append("Check-in");
            if (request.getCheckInMethod() != null) {
                checkInNoteBuilder.append(" via ").append(request.getCheckInMethod());
            }
            if (request.getCheckInLocation() != null) {
                checkInNoteBuilder.append(" at ").append(request.getCheckInLocation());
            }
            if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                checkInNoteBuilder.append(" - ").append(request.getNotes());
            }
        } else if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            checkInNoteBuilder.append("Check-in: ").append(request.getNotes());
        } else if (request.getQrCode() != null && !request.getQrCode().trim().isEmpty()) {
            checkInNoteBuilder.append("Checked in via QR code");
        } else {
            checkInNoteBuilder.append("Checked in at ").append(LocalDateTime.now());
        }
        
        String checkInNote = checkInNoteBuilder.toString();
        attendance.setNotes(existingNotes.isEmpty() ? checkInNote : existingNotes + "\n" + checkInNote);
        
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
        return qrCodeService.scanQRCode(eventId, qrCode);
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
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (request.getRoleName() == null) {
            throw new IllegalArgumentException("Role name is required");
        }
        
        eventValidationUtil.validateEventExists(eventId);
        
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
            if (request.getNotes() != null) {
                role.setNotes(request.getNotes());
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
            role.setNotes(request.getNotes());
        }
        
        // Save the EventRole - this was the bug mentioned in the review
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
    
    // Analytics and Reporting - Delegated to AttendeeAnalyticsService
    public AttendanceSummaryResponse getAttendanceSummary(UUID eventId) {
        return analyticsService.getAttendanceSummary(eventId);
    }
    
    public AttendanceAnalyticsResponse getAttendanceAnalytics(UUID eventId) {
        return analyticsService.getAttendanceAnalytics(eventId);
    }
    
    // Export functionality - Delegated to AttendeeExportService
    public ExportResponse exportAttendees(UUID eventId, String format) {
        return exportService.exportAttendees(eventId, format);
    }
    
    // QR Code operations - Delegated to AttendeeQRCodeService
    public String getAttendeeQRCode(UUID attendanceId) {
        return qrCodeService.getAttendeeQRCode(attendanceId);
    }
    
    public String regenerateQRCode(UUID attendanceId) {
        return qrCodeService.regenerateQRCode(attendanceId);
    }
    
    public AttendeeQRCodeResponse getAttendeeQRCodePayload(UUID attendanceId) {
        return qrCodeService.getAttendeeQRCodePayload(attendanceId);
    }
    
    public AttendeeQRCodeResponse regenerateQRCodePayload(UUID attendanceId) {
        return qrCodeService.regenerateQRCodePayload(attendanceId);
    }
    
    public QRCodeGenerationResult getAttendeeQRCodeImage(UUID attendanceId) {
        return qrCodeService.getAttendeeQRCodeImage(attendanceId);
    }
    
    public QRCodeGenerationResult regenerateQRCodeImage(UUID attendanceId) {
        return qrCodeService.regenerateQRCodeImage(attendanceId);
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
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventUser> eventUsers = eventUserRepository.findByEventId(eventId);
        return eventUsers.stream()
                .map(this::convertToEventUserResponse)
                .collect(Collectors.toList());
    }
    
    public EventUserResponse getEventUser(UUID eventId, UUID userId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        EventUser eventUser = eventUserRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("Event user not found for event: " + eventId + ", user: " + userId));
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
    
    // Analytics Methods - Delegated to AttendeeAnalyticsService
    public List<AttendanceAnalyticsResponse.CheckInTimeline> getCheckInTimeline(UUID eventId) {
        return analyticsService.getCheckInTimeline(eventId);
    }
    
    public Map<String, Long> getAttendanceByType(UUID eventId) {
        return analyticsService.getAttendanceByType(eventId);
    }
    
    public List<AttendanceAnalyticsResponse.NoShowAnalysis> getNoShowAnalytics(UUID eventId) {
        return analyticsService.getNoShowAnalytics(eventId);
    }
    
    public List<AttendanceAnalyticsResponse.RegistrationTimeline> getRegistrationTimeline(UUID eventId) {
        return analyticsService.getRegistrationTimeline(eventId);
    }
    
    // Search and Filter Methods - Delegated to AttendeeSearchService
    public List<AttendanceDetailResponse> searchAttendees(UUID eventId, String name, String email, String status) {
        return searchService.searchAttendees(eventId, name, email, status);
    }
    
    public List<AttendanceDetailResponse> filterAttendees(UUID eventId, String status, String ticketType, Boolean hasDietaryRestrictions) {
        return searchService.filterAttendees(eventId, status, ticketType, hasDietaryRestrictions);
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
    
    // Validation Methods - Delegated to AttendeeSearchService and AttendeeAnalyticsService
    public List<AttendanceDetailResponse> findDuplicateAttendees(UUID eventId) {
        return searchService.findDuplicateAttendees(eventId);
    }
    
    public List<AttendanceDetailResponse> findIncompleteProfiles(UUID eventId) {
        return searchService.findIncompleteProfiles(eventId);
    }
    
    public Map<String, Object> validateAttendeeData(UUID eventId) {
        return searchService.validateAttendeeData(eventId);
    }
    
    public Map<String, Object> getCapacityStatus(UUID eventId) {
        return analyticsService.getCapacityStatus(eventId);
    }
    
    public Map<String, Object> getWaitlistStatus(UUID eventId) {
        return analyticsService.getWaitlistStatus(eventId);
    }
    
    // Communication Methods - Delegated to AttendeeCommunicationService
    public Map<String, Object> sendBulkEmail(UUID eventId, SendInvitationRequest request) {
        return communicationService.sendBulkEmail(eventId, request);
    }
    
    public Map<String, Object> sendNotification(UUID eventId, UUID attendanceId, SendInvitationRequest request) {
        return communicationService.sendNotification(eventId, attendanceId, request);
    }
    
    public List<Map<String, Object>> getCommunicationHistory(UUID eventId) {
        return communicationService.getCommunicationHistory(eventId);
    }
    
    public InvitationResponse sendInvitations(UUID eventId, SendInvitationRequest request) {
        return communicationService.sendInvitations(eventId, request);
    }
    
    public List<InvitationResponse> getSentInvitations(UUID eventId) {
        return communicationService.getSentInvitations(eventId);
    }
    
    // Export/Import Methods - Delegated to AttendeeExportService
    public List<AttendanceDetailResponse> importAttendeesCSV(UUID eventId, String csvData) {
        return exportService.importAttendeesCSV(eventId, csvData);
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
