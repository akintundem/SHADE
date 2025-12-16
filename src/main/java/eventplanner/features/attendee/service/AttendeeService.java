package eventplanner.features.attendee.service;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.features.attendee.dto.request.BulkAttendeeCreateRequest;
import eventplanner.features.attendee.dto.response.AttendeeResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for attendee management operations.
 * Handles creation (single or bulk), retrieval, deletion, and filtering of attendees.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttendeeService {
    
    private final AttendeeRepository repository;
    private final EventRepository eventRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    // ==================== Core CRUD Operations ====================

    /**
     * Get attendee by ID
     */
    @Transactional(readOnly = true)
    public Attendee getAttendeeById(UUID attendeeId) {
        return repository.findById(attendeeId)
            .orElseThrow(() -> new IllegalArgumentException("Attendee not found: " + attendeeId));
    }

    /**
     * Delete attendee
     */
    public boolean delete(UUID attendeeId) {
        if (repository.existsById(attendeeId)) {
            repository.deleteById(attendeeId);
            log.info("Deleted attendee {}", attendeeId);
            return true;
        }
        return false;
    }

    // ==================== Create Operations ====================

    /**
     * Create attendees from bulk request (supports single or multiple attendees).
     * Handles user resolution, validation, and optional notifications.
     */
    public List<Attendee> createFromBulkRequest(BulkAttendeeCreateRequest request) {
        if (request == null || request.getEventId() == null) {
            throw new IllegalArgumentException("Invalid request: eventId is required");
        }
        
        if (request.getAttendees() == null || request.getAttendees().isEmpty()) {
            throw new IllegalArgumentException("Attendees list cannot be empty");
        }
        
        // Fetch the event
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + request.getEventId()));
        
        // Map attendee info to Attendee entities, resolving userId or email
        List<Attendee> toSave = request.getAttendees().stream().map(attendeeInfo -> {
            Attendee attendee = new Attendee();
            attendee.setEvent(event);
            
            // If userId is provided, fetch user account and auto-fill info
            if (attendeeInfo.getUserId() != null) {
                UserAccount user = userAccountRepository.findById(attendeeInfo.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with userId: " + attendeeInfo.getUserId()));
                
                // Set user relationship
                attendee.setUser(user);
                
                // Auto-fill from user account (can be overridden)
                attendee.setName(attendeeInfo.getName() != null && !attendeeInfo.getName().trim().isEmpty() 
                    ? attendeeInfo.getName() : user.getName());
                attendee.setEmail(attendeeInfo.getEmail() != null && !attendeeInfo.getEmail().trim().isEmpty()
                    ? attendeeInfo.getEmail() : user.getEmail());
            } else {
                // Use email - validate that either userId or email is provided
                if (attendeeInfo.getEmail() == null || attendeeInfo.getEmail().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        "Either userId or email must be provided for each attendee");
                }
                
                // Try to resolve userId from email (optional - if user exists, link them)
                Optional<UserAccount> userByEmail = userAccountRepository.findByEmailIgnoreCase(attendeeInfo.getEmail());
                if (userByEmail.isPresent()) {
                    UserAccount user = userByEmail.get();
                    // Link user account
                    attendee.setUser(user);
                    // Auto-fill name if not provided
                    attendee.setName(attendeeInfo.getName() != null && !attendeeInfo.getName().trim().isEmpty()
                        ? attendeeInfo.getName() : user.getName());
                } else {
                    // User doesn't exist - use provided info, no user link
                    attendee.setUser(null);
                    if (attendeeInfo.getName() == null || attendeeInfo.getName().trim().isEmpty()) {
                        throw new IllegalArgumentException(
                            "Name is required when email is provided and user doesn't exist in directory");
                    }
                    attendee.setName(attendeeInfo.getName());
                }
                
                attendee.setEmail(attendeeInfo.getEmail());
            }
            
            // Validate attendee before saving
            validateAttendee(attendee);
            
            // Check for duplicate email in the same event
            if (attendee.getEmail() != null && !attendee.getEmail().isEmpty()) {
                Optional<Attendee> existing = repository.findByEventIdAndEmail(event.getId(), attendee.getEmail());
                if (existing.isPresent()) {
                    log.warn("Duplicate attendee detected for email {} in event {}", attendee.getEmail(), event.getId());
                    throw new IllegalArgumentException("An attendee with this email already exists for this event");
                }
            }
            
            return attendee;
        }).collect(Collectors.toList());
        
        // Save all attendees
        List<Attendee> saved = repository.saveAll(toSave);
        log.info("Created {} attendees for event {}", saved.size(), event.getId());
        
        // Send welcome/confirmation communications if requested
        if (Boolean.TRUE.equals(request.getSendEmail()) || 
            Boolean.TRUE.equals(request.getSendPushNotification())) {
            sendAttendeeWelcomeCommunications(event, saved, request);
        }
        
        return saved;
    }

    // ==================== Query Operations ====================

    /**
     * Filter and list attendees with pagination.
     * Supports filtering by status, check-in status, search query, userId, and email.
     */
    @Transactional(readOnly = true)
    public Page<Attendee> filterAttendees(
            UUID eventId,
            String status,
            Boolean checkedIn,
            String search,
            UUID userId,
            String email,
            int page,
            int size,
            String sortBy,
            String sortDirection) {
        
        // Validate and normalize pagination parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100; // Max page size
        
        // Create sort
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Apply filters in priority order
        if (StringUtils.hasText(search)) {
            return repository.searchByEventIdAndEmailOrName(eventId, search.trim(), pageable);
        }
        
        if (checkedIn != null) {
            if (checkedIn) {
                return repository.findCheckedInByEventId(eventId, pageable);
            } else {
                return repository.findNotCheckedInByEventId(eventId, pageable);
            }
        }
        
        if (StringUtils.hasText(status)) {
            List<AttendeeStatus> statuses = parseStatuses(status);
            if (statuses.isEmpty()) {
                throw new IllegalArgumentException("Invalid status values");
            }
            return repository.findByEventIdAndRsvpStatusIn(eventId, statuses, pageable);
        }
        
        if (userId != null) {
            Optional<Attendee> attendeeOpt = repository.findByEventIdAndUserId(eventId, userId);
            if (attendeeOpt.isPresent()) {
                return new PageImpl<>(
                    Collections.singletonList(attendeeOpt.get()), 
                    pageable, 
                    1
                );
            } else {
                return Page.empty(pageable);
            }
        }
        
        if (StringUtils.hasText(email)) {
            Optional<Attendee> attendeeOpt = repository.findByEventIdAndEmail(eventId, email.trim());
            if (attendeeOpt.isPresent()) {
                return new PageImpl<>(
                    Collections.singletonList(attendeeOpt.get()), 
                    pageable, 
                    1
                );
            } else {
                return Page.empty(pageable);
            }
        }
        
        // No filters - return all attendees for the event
        return repository.findByEventId(eventId, pageable);
    }

    // ==================== DTO Conversion ====================

    /**
     * Convert Attendee entity to AttendeeResponse DTO.
     * Handles both user-linked attendees (with userId) and email-only guests (userId is null).
     */
    public AttendeeResponse toResponse(Attendee attendee) {
        return AttendeeResponse.builder()
                .id(attendee.getId())
                .eventId(attendee.getEvent() != null ? attendee.getEvent().getId() : null)
                .userId(attendee.getUser() != null ? attendee.getUser().getId() : null)
                .name(attendee.getName())
                .email(attendee.getEmail())
                .rsvpStatus(attendee.getRsvpStatus())
                .checkedInAt(attendee.getCheckedInAt())
                .createdAt(attendee.getCreatedAt())
                .updatedAt(attendee.getUpdatedAt())
                .build();
    }

    /**
     * Convert list of attendees to list of responses
     */
    public List<AttendeeResponse> toResponseList(List<Attendee> attendees) {
        return attendees.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    /**
     * Validate attendee entity
     */
    private void validateAttendee(Attendee attendee) {
        if (attendee.getEvent() == null) {
            throw new IllegalArgumentException("Event is required");
        }
        if (attendee.getName() == null || attendee.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Attendee name is required");
        }
        // Email is optional but should be valid if provided
        if (attendee.getEmail() != null && !attendee.getEmail().isEmpty()) {
            if (!isValidEmail(attendee.getEmail())) {
                throw new IllegalArgumentException("Invalid email format");
            }
        }
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Parse comma-separated status values
     */
    private List<AttendeeStatus> parseStatuses(String statusParam) {
        if (!StringUtils.hasText(statusParam)) {
            return Collections.emptyList();
        }
        
        List<AttendeeStatus> statuses = new ArrayList<>();
        String[] parts = statusParam.toUpperCase().split(",");
        
        for (String part : parts) {
            String trimmed = part.trim();
            try {
                AttendeeStatus status = AttendeeStatus.valueOf(trimmed);
                statuses.add(status);
            } catch (IllegalArgumentException e) {
                // Skip invalid status values
                log.warn("Invalid attendee status value: {}", trimmed);
            }
        }
        
        return statuses;
    }

    /**
     * Send welcome communications to newly created attendees based on notification preferences
     */
    private void sendAttendeeWelcomeCommunications(Event event, List<Attendee> attendees, BulkAttendeeCreateRequest request) {
        if (event == null || attendees == null || attendees.isEmpty()) {
            return;
        }
        
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("eventName", event.getName());
        templateVariables.put("eventId", event.getId().toString());
        if (event.getStartDateTime() != null) {
            templateVariables.put("eventStartDate", event.getStartDateTime().toString());
        }
        if (event.getVenueRequirements() != null) {
            templateVariables.put("eventVenue", event.getVenueRequirements());
        }
        
        boolean sendEmail = Boolean.TRUE.equals(request.getSendEmail());
        boolean sendPush = Boolean.TRUE.equals(request.getSendPushNotification());
        
        for (Attendee attendee : attendees) {
            try {
                // Send email if requested and email is available
                if (sendEmail && attendee.getEmail() != null && !attendee.getEmail().trim().isEmpty()) {
                    notificationService.send(NotificationRequest.builder()
                            .type(CommunicationType.EMAIL)
                            .to(attendee.getEmail())
                            .subject("You've been added to: " + event.getName())
                            .templateId("attendee-welcome")
                            .templateVariables(templateVariables)
                            .eventId(event.getId())
                            .build());
                }
                
                // Send push notification if requested and user account is linked
                if (sendPush && attendee.getUser() != null && attendee.getUser().getId() != null) {
                    Map<String, Object> pushData = new HashMap<>(templateVariables);
                    pushData.put("body", "You've been added as an attendee to: " + event.getName());
                    
                    notificationService.send(NotificationRequest.builder()
                            .type(CommunicationType.PUSH_NOTIFICATION)
                            .to(attendee.getUser().getId().toString())
                            .subject("Event attendance confirmed")
                            .templateVariables(pushData)
                            .eventId(event.getId())
                            .build());
                }
            } catch (Exception e) {
                // Don't fail the entire operation if communication fails
                log.warn("Failed to send notification to attendee {}: {}", attendee.getId(), e.getMessage());
            }
        }
    }
}
