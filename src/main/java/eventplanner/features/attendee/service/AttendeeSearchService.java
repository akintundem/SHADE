package eventplanner.features.attendee.service;

import eventplanner.features.attendee.dto.response.AttendanceDetailResponse;
import eventplanner.features.attendee.entity.EventAttendance;
import eventplanner.features.attendee.repository.EventAttendanceRepository;
import eventplanner.common.domain.enums.AttendanceStatus;
import eventplanner.common.util.EventValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for attendee search, filter, and validation operations.
 * All methods validate event existence and ensure proper authorization context.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AttendeeSearchService {
    
    private final EventAttendanceRepository attendanceRepository;
    private final EventValidationUtil eventValidationUtil;
    
    /**
     * Convert EventAttendance to AttendanceDetailResponse
     */
    private AttendanceDetailResponse convertToDetailResponse(EventAttendance attendance) {
        return new AttendanceDetailResponse(
                attendance.getId(),
                attendance.getEvent() != null ? attendance.getEvent().getId() : null,
                attendance.getUser() != null ? attendance.getUser().getId() : null,
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
    
    /**
     * Search attendees by name, email, or status.
     * Authorization: Enforced at controller level via RBAC (attendee.read permission)
     * 
     * @param eventId The event ID
     * @param name Optional name search term
     * @param email Optional email search term
     * @param status Optional status filter
     * @return List of matching attendees
     */
    public List<AttendanceDetailResponse> searchAttendees(UUID eventId, String name, String email, String status) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
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
    
    /**
     * Filter attendees by status, ticket type, or dietary restrictions.
     * Authorization: Enforced at controller level via RBAC (attendee.read permission)
     * 
     * @param eventId The event ID
     * @param status Optional status filter
     * @param ticketType Optional ticket type filter
     * @param hasDietaryRestrictions Optional dietary restrictions filter
     * @return List of filtered attendees
     */
    public List<AttendanceDetailResponse> filterAttendees(UUID eventId, String status, String ticketType, Boolean hasDietaryRestrictions) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
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
    
    /**
     * Find duplicate attendees (by email).
     * Authorization: Enforced at controller level via RBAC (attendee.read permission)
     * 
     * @param eventId The event ID
     * @return List of duplicate attendees
     */
    public List<AttendanceDetailResponse> findDuplicateAttendees(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
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
    
    /**
     * Find attendees with incomplete profiles.
     * Authorization: Enforced at controller level via RBAC (attendee.read permission)
     * 
     * @param eventId The event ID
     * @return List of attendees with incomplete profiles
     */
    public List<AttendanceDetailResponse> findIncompleteProfiles(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        return attendances.stream()
                .filter(a -> a.getName() == null || a.getName().trim().isEmpty() ||
                           a.getEmail() == null || a.getEmail().trim().isEmpty() ||
                           a.getPhone() == null || a.getPhone().trim().isEmpty())
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Validate attendee data for an event.
     * Authorization: Enforced at controller level via RBAC (attendee.validate permission)
     * 
     * @param eventId The event ID
     * @return Map with validation results
     */
    public Map<String, Object> validateAttendeeData(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
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
        
        @SuppressWarnings("unchecked")
        List<String> issues = (List<String>) result.get("issues");
        if (incomplete > 0) {
            issues.add(incomplete + " attendee(s) have incomplete profiles");
        }
        if (duplicates > 0) {
            issues.add(duplicates + " duplicate record(s) found");
        }
        
        return result;
    }
    
    /**
     * Count duplicate attendees by email
     */
    private long findDuplicateCount(List<EventAttendance> attendances) {
        Map<String, Long> emailCounts = attendances.stream()
                .filter(a -> a.getEmail() != null && !a.getEmail().trim().isEmpty())
                .collect(Collectors.groupingBy(EventAttendance::getEmail, Collectors.counting()));
        
        return emailCounts.values().stream()
                .filter(count -> count > 1)
                .mapToLong(count -> count - 1) // Count duplicates (excluding first occurrence)
                .sum();
    }
}

