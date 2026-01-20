package eventplanner.features.attendee.service;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.enums.CommunicationType;
import eventplanner.features.event.enums.EventAccessType;
import eventplanner.features.event.enums.EventStatus;
import eventplanner.security.auth.enums.VisibilityLevel;
import eventplanner.features.attendee.dto.request.AttendeeInfo;
import eventplanner.features.attendee.dto.request.BulkAttendeeCreateRequest;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.entity.AttendeeRsvpHistory;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.attendee.enums.RsvpChangeSource;
import eventplanner.features.attendee.dto.request.BulkRsvpUpdateItem;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.attendee.repository.AttendeeRsvpHistoryRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.event.service.EventWaitlistService;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.common.config.ExternalServicesProperties;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.util.AuthValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final ExternalServicesProperties externalServicesProperties;
    private final AuthorizationService authorizationService;
    private final AttendeeRsvpHistoryRepository rsvpHistoryRepository;
    
    @Autowired(required = false)
    private EventWaitlistService eventWaitlistService;

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
        Attendee attendee = repository.findById(attendeeId).orElse(null);
        if (attendee == null) {
            return false;
        }

        Event event = attendee.getEvent();
        AttendeeStatus previousStatus = attendee.getRsvpStatus();
        
        // Delete the attendee
        repository.deleteById(attendeeId);
        log.info("Deleted attendee {}", attendeeId);
        
        // Update event attendee count if attendee was confirmed
        if (event != null && previousStatus == AttendeeStatus.CONFIRMED) {
            try {
                Integer currentCount = event.getCurrentAttendeeCount() != null ? event.getCurrentAttendeeCount() : 0;
                event.setCurrentAttendeeCount(Math.max(0, currentCount - 1));
                eventRepository.save(event);
                
                // Promote waitlist entries when capacity becomes available
                if (eventWaitlistService != null && event.getCapacity() != null) {
                    try {
                        int spotsAvailable = 1; // One attendee deleted
                        eventWaitlistService.promoteWaitlistEntries(event.getId(), spotsAvailable);
                    } catch (Exception e) {
                        log.warn("Failed to promote waitlist entries after attendee deletion for event {}: {}", 
                                event.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to update event count after attendee deletion for event {}: {}", 
                        event != null ? event.getId() : null, e.getMessage());
            }
        }
        
        return true;
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

            String trimmedEmail = AuthValidationUtil.safeTrim(attendeeInfo.getEmail());
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
                String resolvedEmail = StringUtils.hasText(trimmedEmail)
                    ? AuthValidationUtil.normalizeEmail(trimmedEmail)
                    : AuthValidationUtil.normalizeEmail(resolvedUser.getEmail());
                attendee.setEmail(resolvedEmail);
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
                    attendee.setEmail(AuthValidationUtil.normalizeEmail(trimmedEmail));
                } else {
                    // User doesn't exist - use provided info, no user link
                    attendee.setUser(null);
                    if (!StringUtils.hasText(attendeeInfo.getName())) {
                        throw new IllegalArgumentException(
                            "Name is required when email is provided and user doesn't exist in directory");
                    }
                    attendee.setName(attendeeInfo.getName().trim());
                    attendee.setEmail(AuthValidationUtil.normalizeEmail(trimmedEmail));
                }
            }

            UUID resolvedUserId = resolvedUser != null ? resolvedUser.getId() : null;
            String normalizedEmail = attendee.getEmail();

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
            AuthValidationUtil.normalizeEmail(attendee.getEmail());
        }
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
                            .from(externalServicesProperties.getEmail().getFromEvents())
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
     * Creates or updates an attendee record with CONFIRMED or PENDING status.
     *
     * @param eventId The event ID
     * @param principal The authenticated user
     * @return The created or updated attendee
     */
    public Attendee rsvpToEvent(UUID eventId, UserPrincipal principal) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        // Verify event requires RSVP
        if (event.getAccessType() != EventAccessType.RSVP_REQUIRED) {
            throw new IllegalArgumentException("Event does not require RSVP");
        }

        ensureEventOpenForRsvp(event);
        ensurePrivateEventAccess(event, principal);

        UserAccount user = userAccountRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getId()));

        AttendeeStatus targetStatus = Boolean.TRUE.equals(event.getRequiresApproval())
                ? AttendeeStatus.PENDING
                : AttendeeStatus.CONFIRMED;

        Optional<Attendee> existingAttendee = repository.findByEventIdAndUserId(eventId, principal.getId());
        if (existingAttendee.isPresent()) {
            Attendee attendee = existingAttendee.get();
            AttendeeStatus previousStatus = attendee.getRsvpStatus();
            AttendeeStatus resolvedStatus = resolveApprovalStatus(event, previousStatus, targetStatus);
            if (resolvedStatus == AttendeeStatus.CONFIRMED && previousStatus != AttendeeStatus.CONFIRMED) {
                ensureCapacityAvailable(event);
            }
            attendee.setRsvpStatus(resolvedStatus);
            Attendee saved = repository.save(attendee);
            updateEventAttendeeCount(event, previousStatus, resolvedStatus);
            recordRsvpHistory(saved, previousStatus, resolvedStatus, principal, RsvpChangeSource.USER, null);
            return saved;
        }

        ensureCapacityAvailable(event);

        Attendee attendee = new Attendee();
        attendee.setEvent(event);
        attendee.setUser(user);
        attendee.setName(user.getName());
        attendee.setEmail(user.getEmail());
        attendee.setRsvpStatus(targetStatus);

        Attendee saved = repository.save(attendee);
        updateEventAttendeeCount(event, null, targetStatus);
        recordRsvpHistory(saved, null, targetStatus, principal, RsvpChangeSource.USER, null);
        return saved;
    }

    /**
     * Update RSVP status for the authenticated user.
     */
    public Attendee updateRsvpStatus(UUID eventId, AttendeeStatus status, UserPrincipal principal) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (status == null) {
            throw new IllegalArgumentException("RSVP status is required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        ensureEventOpenForRsvp(event);
        ensurePrivateEventAccess(event, principal);

        Attendee attendee = repository.findByEventIdAndUserId(eventId, principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("RSVP not found for event: " + eventId));

        AttendeeStatus previousStatus = attendee.getRsvpStatus();
        AttendeeStatus resolvedStatus = resolveApprovalStatus(event, previousStatus, status);
        if (resolvedStatus == AttendeeStatus.CONFIRMED && previousStatus != AttendeeStatus.CONFIRMED) {
            ensureCapacityAvailable(event);
        }

        attendee.setRsvpStatus(resolvedStatus);
        Attendee saved = repository.save(attendee);
        updateEventAttendeeCount(event, previousStatus, resolvedStatus);
        recordRsvpHistory(saved, previousStatus, resolvedStatus, principal, RsvpChangeSource.USER, null);
        return saved;
    }

    /**
     * Cancel RSVP for the authenticated user.
     */
    public Attendee cancelRsvp(UUID eventId, UserPrincipal principal) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        ensurePrivateEventAccess(event, principal);

        Attendee attendee = repository.findByEventIdAndUserId(eventId, principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("RSVP not found for event: " + eventId));

        AttendeeStatus previousStatus = attendee.getRsvpStatus();
        if (previousStatus == AttendeeStatus.DECLINED) {
            return attendee;
        }

        attendee.setRsvpStatus(AttendeeStatus.DECLINED);
        Attendee saved = repository.save(attendee);
        updateEventAttendeeCount(event, previousStatus, AttendeeStatus.DECLINED);
        recordRsvpHistory(saved, previousStatus, AttendeeStatus.DECLINED, principal, RsvpChangeSource.USER, null);
        return saved;
    }

    /**
     * Bulk update RSVP statuses for an event (organizer/admin use).
     */
    public List<Attendee> bulkUpdateRsvpStatus(UUID eventId, List<BulkRsvpUpdateItem> updates, String note, UserPrincipal principal) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("At least one RSVP update is required");
        }

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        if (event.getAccessType() != EventAccessType.RSVP_REQUIRED) {
            throw new IllegalArgumentException("Event does not require RSVP");
        }

        ensureEventManagerAccess(event, principal);
        ensureEventOpenForRsvp(event);

        boolean canOverrideApproval = canManageEvent(event, principal);

        List<Attendee> updated = new ArrayList<>();
        for (BulkRsvpUpdateItem item : updates) {
            Attendee attendee = repository.findByIdAndEventId(item.getAttendeeId(), eventId)
                .orElseThrow(() -> new IllegalArgumentException("Attendee not found: " + item.getAttendeeId()));

            AttendeeStatus previousStatus = attendee.getRsvpStatus();
            AttendeeStatus resolvedStatus = canOverrideApproval
                ? item.getStatus()
                : resolveApprovalStatus(event, previousStatus, item.getStatus());

            if (resolvedStatus == AttendeeStatus.CONFIRMED && previousStatus != AttendeeStatus.CONFIRMED) {
                ensureCapacityAvailable(event);
            }
            attendee.setRsvpStatus(resolvedStatus);
            Attendee saved = repository.save(attendee);
            updateEventAttendeeCount(event, previousStatus, resolvedStatus);
            recordRsvpHistory(saved, previousStatus, resolvedStatus, principal, RsvpChangeSource.ORGANIZER, note);
            updated.add(saved);
        }

        return updated;
    }

    /**
     * Get RSVP status for the authenticated user.
     */
    @Transactional(readOnly = true)
    public Optional<Attendee> getRsvpStatus(UUID eventId, UserPrincipal principal) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        ensurePrivateEventAccess(event, principal);

        return repository.findByEventIdAndUserId(eventId, principal.getId());
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

    private void ensureEventOpenForRsvp(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event not found");
        }
        if (event.getEventStatus() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Event has been cancelled");
        }
        if (event.getEventStatus() == EventStatus.REGISTRATION_CLOSED) {
            throw new IllegalArgumentException("Event registration is closed");
        }
        if (event.getEventStatus() == EventStatus.COMPLETED) {
            throw new IllegalArgumentException("Event has ended");
        }
        if (isEventPast(event)) {
            throw new IllegalArgumentException("Event has ended");
        }
        if (event.getRegistrationDeadline() != null &&
                LocalDateTime.now(ZoneOffset.UTC).isAfter(event.getRegistrationDeadline())) {
            throw new IllegalArgumentException("Registration deadline has passed");
        }
    }

    private void ensurePrivateEventAccess(Event event, UserPrincipal principal) {
        if (authorizationService.canAccessEventWithInvite(principal, event)) {
            return;
        }
        throw new IllegalArgumentException("Access denied for private event");
    }

    private void ensureEventManagerAccess(Event event, UserPrincipal principal) {
        if (event == null || principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (canManageEvent(event, principal)) {
            return;
        }
        throw new IllegalArgumentException("Access denied to manage RSVP");
    }

    private boolean canManageEvent(Event event, UserPrincipal principal) {
        if (event == null || principal == null) {
            return false;
        }
        if (authorizationService.isAdmin(principal) || authorizationService.isEventOwner(principal, event.getId())) {
            return true;
        }
        return authorizationService.hasEventMembership(principal, event.getId());
    }

    private void ensureCapacityAvailable(Event event) {
        if (event == null) {
            return;
        }
        Integer capacity = event.getCapacity();
        if (capacity == null || capacity <= 0) {
            return;
        }
        Integer current = event.getCurrentAttendeeCount();
        if (current == null) {
            current = 0;
        }
        if (current >= capacity) {
            throw new IllegalArgumentException("Event is at capacity");
        }
    }

    private AttendeeStatus resolveApprovalStatus(Event event, AttendeeStatus previousStatus, AttendeeStatus requestedStatus) {
        if (!Boolean.TRUE.equals(event.getRequiresApproval())) {
            return requestedStatus;
        }
        if (previousStatus == AttendeeStatus.CONFIRMED) {
            return AttendeeStatus.CONFIRMED;
        }
        if (requestedStatus == AttendeeStatus.CONFIRMED) {
            return AttendeeStatus.PENDING;
        }
        return requestedStatus;
    }

    private void updateEventAttendeeCount(Event event, AttendeeStatus previousStatus, AttendeeStatus nextStatus) {
        if (event == null) {
            return;
        }
        if (event.getCurrentAttendeeCount() == null) {
            event.setCurrentAttendeeCount(0);
        }
        boolean wasConfirmed = previousStatus == AttendeeStatus.CONFIRMED;
        boolean isConfirmed = nextStatus == AttendeeStatus.CONFIRMED;
        if (!wasConfirmed && isConfirmed) {
            event.setCurrentAttendeeCount(event.getCurrentAttendeeCount() + 1);
            eventRepository.save(event);
        } else if (wasConfirmed && !isConfirmed) {
            event.setCurrentAttendeeCount(Math.max(0, event.getCurrentAttendeeCount() - 1));
            eventRepository.save(event);
            
            // Promote waitlist entries when capacity becomes available
            if (eventWaitlistService != null && event.getCapacity() != null) {
                try {
                    // Calculate how many spots became available
                    int spotsAvailable = 1; // One attendee cancelled
                    eventWaitlistService.promoteWaitlistEntries(event.getId(), spotsAvailable);
                } catch (Exception e) {
                    // Log but don't fail the attendee update
                    log.warn("Failed to promote waitlist entries for event {}: {}", 
                            event.getId(), e.getMessage());
                }
            }
        }
    }

    private void recordRsvpHistory(Attendee attendee, AttendeeStatus previousStatus, AttendeeStatus newStatus,
                                   UserPrincipal principal, RsvpChangeSource source, String note) {
        if (attendee == null || attendee.getEvent() == null || newStatus == null) {
            return;
        }
        if (previousStatus == newStatus) {
            return;
        }
        AttendeeRsvpHistory history = new AttendeeRsvpHistory();
        history.setEvent(attendee.getEvent());
        history.setAttendee(attendee);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setSource(source != null ? source : RsvpChangeSource.USER);
        history.setNote(note != null && !note.isBlank() ? note.trim() : null);
        if (principal != null && principal.getUser() != null) {
            history.setChangedBy(principal.getUser());
        }
        rsvpHistoryRepository.save(history);
    }

    private boolean isEventPast(Event event) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (event.getEndDateTime() != null) {
            return now.isAfter(event.getEndDateTime());
        }
        if (event.getStartDateTime() != null) {
            return now.isAfter(event.getStartDateTime().plusDays(1));
        }
        return false;
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
