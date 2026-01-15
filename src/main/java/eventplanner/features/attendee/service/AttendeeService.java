package eventplanner.features.attendee.service;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.common.domain.enums.EventAccessType;
import eventplanner.common.domain.enums.VisibilityLevel;
import eventplanner.features.attendee.dto.request.AttendeeInfo;
import eventplanner.features.attendee.dto.request.BulkAttendeeCreateRequest;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${external.email.from.events:events@noreply.mayokun.dev}")
    private String eventsFrom;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "name", "email", "rsvpStatus", "checkedInAt", "createdAt"
    );

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
        
        // Track duplicates within the same request to fail fast
        Set<UUID> seenUserIds = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();

        List<Attendee> toSave = new ArrayList<>();

        for (AttendeeInfo attendeeInfo : request.getAttendees()) {
            Attendee attendee = new Attendee();
            attendee.setEvent(event);

            String trimmedEmail = attendeeInfo.getEmail() != null ? attendeeInfo.getEmail().trim() : null;
            UserAccount resolvedUser = null;

            // If userId is provided, fetch user account and auto-fill info
            if (attendeeInfo.getUserId() != null) {
                resolvedUser = userAccountRepository.findById(attendeeInfo.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with userId: " + attendeeInfo.getUserId()));
                attendee.setUser(resolvedUser);

                attendee.setName(StringUtils.hasText(attendeeInfo.getName())
                    ? attendeeInfo.getName().trim()
                    : resolvedUser.getName());
                attendee.setEmail(StringUtils.hasText(trimmedEmail)
                    ? trimmedEmail
                    : resolvedUser.getEmail());
            } else {
                // Use email - validate that either userId or email is provided
                if (!StringUtils.hasText(trimmedEmail)) {
                    throw new IllegalArgumentException(
                        "Either userId or email must be provided for each attendee");
                }

                // Try to resolve userId from email (optional - if user exists, link them)
                Optional<UserAccount> userByEmail = userAccountRepository.findByEmailIgnoreCase(trimmedEmail);
                if (userByEmail.isPresent()) {
                    resolvedUser = userByEmail.get();
                    attendee.setUser(resolvedUser);
                    attendee.setName(StringUtils.hasText(attendeeInfo.getName())
                        ? attendeeInfo.getName().trim()
                        : resolvedUser.getName());
                    attendee.setEmail(trimmedEmail);
                } else {
                    // User doesn't exist - use provided info, no user link
                    attendee.setUser(null);
                    if (!StringUtils.hasText(attendeeInfo.getName())) {
                        throw new IllegalArgumentException(
                            "Name is required when email is provided and user doesn't exist in directory");
                    }
                    attendee.setName(attendeeInfo.getName().trim());
                    attendee.setEmail(trimmedEmail);
                }
            }

            UUID resolvedUserId = resolvedUser != null ? resolvedUser.getId() : null;
            String normalizedEmail = normalizeEmail(attendee.getEmail());

            // Prevent duplicates within the same request (user or email)
            if (resolvedUserId != null && !seenUserIds.add(resolvedUserId)) {
                throw new IllegalArgumentException("Duplicate attendee for userId " + resolvedUserId + " in the same request");
            }
            if (normalizedEmail != null && !seenEmails.add(normalizedEmail)) {
                throw new IllegalArgumentException("Duplicate attendee email in the same request: " + attendee.getEmail());
            }

            // Prevent duplicates against existing attendees in the event
            if (resolvedUserId != null && repository.findByEventIdAndUserId(event.getId(), resolvedUserId).isPresent()) {
                log.warn("Duplicate attendee detected for user {} in event {}", resolvedUserId, event.getId());
                throw new IllegalArgumentException("An attendee with this user already exists for this event");
            }
            if (normalizedEmail != null && repository.findByEventIdAndEmailIgnoreCase(event.getId(), normalizedEmail).isPresent()) {
                log.warn("Duplicate attendee detected for email {} in event {}", attendee.getEmail(), event.getId());
                throw new IllegalArgumentException("An attendee with this email already exists for this event");
            }

            // Set participation visibility: use provided value, or default to user's global setting, or PUBLIC
            VisibilityLevel visibility = attendeeInfo.getParticipationVisibility();
            if (visibility == null && attendee.getUser() != null && attendee.getUser().getSettings() != null) {
                // Use user's global default if not specified
                visibility = attendee.getUser().getSettings().getEventParticipationVisibility();
            }
            if (visibility == null) {
                // Final fallback to PUBLIC
                visibility = VisibilityLevel.PUBLIC;
            }
            attendee.setParticipationVisibility(visibility);

            // Validate attendee before saving
            validateAttendee(attendee);

            toSave.add(attendee);
        }
        
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

        // Validate sort
        if (!StringUtils.hasText(sortBy)) {
            sortBy = "name";
        }
        String normalizedSortBy = sortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(normalizedSortBy)) {
            throw new IllegalArgumentException("Invalid sort field. Allowed values: " + String.join(",", ALLOWED_SORT_FIELDS));
        }

        Sort.Direction direction;
        if (!StringUtils.hasText(sortDirection) || "ASC".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        } else if ("DESC".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.DESC;
        } else {
            throw new IllegalArgumentException("Invalid sort direction. Allowed values: ASC or DESC");
        }

        Sort sort = Sort.by(direction, normalizedSortBy);
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
        // Validate email when provided
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
     * Normalize email for duplicate detection.
     */
    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase() : null;
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
            templateVariables.put("eventDate", event.getStartDateTime().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a")));
        }
        if (event.getVenueRequirements() != null) {
            templateVariables.put("venue", event.getVenueRequirements());
        }
        if (event.getEventWebsiteUrl() != null) {
            templateVariables.put("actionUrl", event.getEventWebsiteUrl());
        }
        
        boolean sendEmail = Boolean.TRUE.equals(request.getSendEmail());
        boolean sendPush = Boolean.TRUE.equals(request.getSendPushNotification());
        
        for (Attendee attendee : attendees) {
            try {
                // Send email if requested and email is available
                if (sendEmail && attendee.getEmail() != null && !attendee.getEmail().trim().isEmpty()) {
                    Map<String, Object> emailVars = new HashMap<>(templateVariables);
                    String attendeeName = attendee.getName() != null && !attendee.getName().isBlank()
                            ? attendee.getName()
                            : "there";
                    emailVars.put("attendeeName", attendeeName);
                    notificationService.send(NotificationRequest.builder()
                            .type(CommunicationType.EMAIL)
                            .to(attendee.getEmail())
                            .subject("You've been added to: " + event.getName())
                            .templateId("attendee-welcome")
                            .templateVariables(emailVars)
                            .eventId(event.getId())
                            .from(eventsFrom)
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

    // ==================== RSVP Operations ====================

    /**
     * RSVP to an event that requires RSVP.
     * Creates or updates an attendee record with CONFIRMED status.
     * 
     * @param eventId The event ID
     * @param userId The user ID RSVPing
     * @return The created or updated attendee
     */
    public Attendee rsvpToEvent(UUID eventId, UUID userId) {
        if (eventId == null || userId == null) {
            throw new IllegalArgumentException("Event ID and User ID are required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        // Verify event requires RSVP
        if (event.getAccessType() != EventAccessType.RSVP_REQUIRED) {
            throw new IllegalArgumentException("Event does not require RSVP");
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Check if user is already an attendee
        Optional<Attendee> existingAttendee = repository.findByEventIdAndUserId(eventId, userId);

        if (existingAttendee.isPresent()) {
            // Update existing attendee to CONFIRMED
            Attendee attendee = existingAttendee.get();
            attendee.setRsvpStatus(AttendeeStatus.CONFIRMED);
            return repository.save(attendee);
        } else {
            // Create new attendee with CONFIRMED status
            Attendee attendee = new Attendee();
            attendee.setEvent(event);
            attendee.setUser(user);
            attendee.setName(user.getName());
            attendee.setEmail(user.getEmail());
            attendee.setRsvpStatus(AttendeeStatus.CONFIRMED);
            
            Attendee saved = repository.save(attendee);
            
            // Update event attendee count
            if (event.getCurrentAttendeeCount() == null) {
                event.setCurrentAttendeeCount(0);
            }
            event.setCurrentAttendeeCount(event.getCurrentAttendeeCount() + 1);
            eventRepository.save(event);
            
            return saved;
        }
    }

    /**
     * Get count of confirmed attendees for an event.
     * 
     * @param eventId The event ID
     * @return Count of confirmed attendees
     */
    @Transactional(readOnly = true)
    public int getConfirmedAttendeeCount(UUID eventId) {
        if (eventId == null) {
            return 0;
        }
        try {
            List<Attendee> confirmedAttendees = repository.findByEventId(eventId).stream()
                    .filter(a -> a.getRsvpStatus() == AttendeeStatus.CONFIRMED)
                    .collect(java.util.stream.Collectors.toList());
            return confirmedAttendees.size();
        } catch (Exception e) {
            log.warn("Failed to get attendee count for event {}: {}", eventId, e.getMessage());
            return 0;
        }
    }

    // ==================== Event Queries ====================

    /**
     * Get events where user has been invited as an attendee (but not as owner).
     * Returns events where the user is an attendee, excluding events they own.
     * 
     * @param userId The user ID
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of events where user is an attendee
     */
    @Transactional(readOnly = true)
    public Page<Event> getInvitedEvents(UUID userId, Integer page, Integer size) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        int pageNum = Math.max(0, page != null ? page : 0);
        int pageSize = Math.min(100, Math.max(1, size != null ? size : 20));
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "event.startDateTime"));
        
        // Get all attendees for this user
        List<Attendee> attendees = repository.findByUserId(userId);
        
        // Extract unique events where user is attendee but not owner, sorted by start date
        List<Event> allMatchingEvents = attendees.stream()
                .map(Attendee::getEvent)
                .filter(event -> event != null && event.getOwner() != null && !event.getOwner().getId().equals(userId))
                .distinct()
                .sorted((e1, e2) -> {
                    if (e1.getStartDateTime() == null && e2.getStartDateTime() == null) return 0;
                    if (e1.getStartDateTime() == null) return 1;
                    if (e2.getStartDateTime() == null) return -1;
                    return e2.getStartDateTime().compareTo(e1.getStartDateTime());
                })
                .collect(java.util.stream.Collectors.toList());
        
        // Get total count before pagination
        long totalCount = allMatchingEvents.size();
        
        // Apply pagination
        List<Event> paginatedEvents = allMatchingEvents.stream()
                .skip((long) pageNum * pageSize)
                .limit(pageSize)
                .collect(java.util.stream.Collectors.toList());
        
        return new PageImpl<>(paginatedEvents, pageable, totalCount);
    }
}
