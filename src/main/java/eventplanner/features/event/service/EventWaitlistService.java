package eventplanner.features.event.service;

import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ConflictException;
import eventplanner.common.exception.exceptions.ForbiddenException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.security.auth.enums.VisibilityLevel;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.event.dto.request.CreateEventWaitlistRequest;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventWaitlistEntry;
import eventplanner.features.event.enums.EventWaitlistStatus;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.event.repository.EventWaitlistEntryRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.util.AuthValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing event waitlist entries.
 * Handles joining waitlist, automatic promotion when capacity opens, and waitlist management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EventWaitlistService {

    private final EventWaitlistEntryRepository waitlistRepository;
    private final EventRepository eventRepository;
    private final AttendeeRepository attendeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthorizationService authorizationService;

    /**
     * Create a waitlist entry for an event.
     * User can join waitlist when event is at capacity.
     */
    public EventWaitlistEntry createEntry(UUID eventId, CreateEventWaitlistRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        // Check if event is open for registration
        if (event.getEventStatus() == null || 
            event.getEventStatus().name().equals("CANCELLED") ||
            event.getEventStatus().name().equals("COMPLETED")) {
            throw new BadRequestException("Event is not open for registration");
        }

        // Check if event is at capacity
        Integer capacity = event.getCapacity();
        Integer currentCount = event.getCurrentAttendeeCount() != null ? event.getCurrentAttendeeCount() : 0;
        
        if (capacity == null || currentCount < capacity) {
            throw new ConflictException("Event is not at capacity; you can register directly");
        }

        // Resolve requester information
        UUID requesterId = null;
        final String requesterEmail;
        final String requesterName;

        if (principal != null && principal.getId() != null) {
            requesterId = principal.getId();
            UserAccount user = userAccountRepository.findById(requesterId)
                .orElseThrow(() -> new BadRequestException("User not found"));
            String userEmail = AuthValidationUtil.normalizeEmail(user.getEmail());
            String userName = user.getName() != null && !user.getName().isBlank() 
                ? user.getName().trim() 
                : userEmail;
            requesterEmail = userEmail;
            requesterName = userName;
        } else {
            // Guest user - require email and name
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new BadRequestException("Email is required for guest waitlist entries");
            }
            if (request.getName() == null || request.getName().isBlank()) {
                throw new BadRequestException("Name is required for guest waitlist entries");
            }
            requesterEmail = AuthValidationUtil.normalizeEmail(request.getEmail());
            requesterName = request.getName().trim();
        }

        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new BadRequestException("Requester email is required");
        }

        // Check if user is already on waitlist
        if (requesterId != null) {
            if (waitlistRepository.existsByEventIdAndRequesterIdAndStatus(
                    eventId, requesterId, EventWaitlistStatus.WAITING)) {
                throw new ConflictException("You are already on the waitlist for this event");
            }
        } else {
            // For guest users, check by email
            final String emailToCheck = requesterEmail;
            List<EventWaitlistEntry> existing = waitlistRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                eventId, EventWaitlistStatus.WAITING);
            boolean existsByEmail = existing.stream()
                .anyMatch(e -> e.getRequesterEmail() != null && 
                             e.getRequesterEmail().equalsIgnoreCase(emailToCheck));
            if (existsByEmail) {
                throw new ConflictException("This email is already on the waitlist for this event");
            }
        }

        // Create waitlist entry
        EventWaitlistEntry entry = new EventWaitlistEntry();
        entry.setEvent(event);
        if (requesterId != null) {
            entry.setRequester(userAccountRepository.findById(requesterId).orElse(null));
        }
        entry.setRequesterEmail(requesterEmail);
        entry.setRequesterName(requesterName);
        entry.setStatus(EventWaitlistStatus.WAITING);

        return waitlistRepository.save(entry);
    }

    /**
     * List waitlist entries for an event.
     */
    @Transactional(readOnly = true)
    public Page<EventWaitlistEntry> listEntries(UUID eventId, EventWaitlistStatus status, Pageable pageable, UserPrincipal principal) {
        // Check access
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found: " + eventId);
        }
        
        if (authorizationService != null && principal != null) {
            if (!authorizationService.canAccessEvent(principal, eventId)) {
                throw new ForbiddenException("Access denied to event: " + eventId);
            }
        }

        if (status == null) {
            return waitlistRepository.findByEventId(eventId, pageable);
        }
        return waitlistRepository.findByEventIdAndStatus(eventId, status, pageable);
    }

    /**
     * Get waitlist entries for the current user.
     */
    @Transactional(readOnly = true)
    public List<EventWaitlistEntry> listEntriesForUser(UUID eventId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Authentication required");
        }
        return waitlistRepository.findByRequesterIdAndEventId(principal.getId(), eventId);
    }

    /**
     * Cancel a waitlist entry.
     */
    public EventWaitlistEntry cancelEntry(UUID eventId, UUID entryId, UserPrincipal principal) {
        EventWaitlistEntry entry = waitlistRepository.findByIdAndEventId(entryId, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found: " + entryId));

        // Check if user can cancel this entry
        if (principal == null || principal.getId() == null) {
            throw new ForbiddenException("Authentication required");
        }

        // User can cancel their own entry, or event managers can cancel any entry
        boolean canCancel = false;
        if (entry.getRequester() != null && entry.getRequester().getId().equals(principal.getId())) {
            canCancel = true;
        } else if (authorizationService != null && authorizationService.canAccessEvent(principal, eventId)) {
            // Event managers can cancel any entry
            canCancel = true;
        }

        if (!canCancel) {
            throw new ForbiddenException("Access denied to waitlist entry");
        }

        try {
            entry.cancel();
        } catch (IllegalStateException e) {
            throw new ConflictException(e.getMessage());
        }
        return waitlistRepository.save(entry);
    }

    /**
     * Automatically promote waitlist entries when capacity becomes available.
     * This should be called when an attendee cancels or capacity increases.
     * 
     * @param eventId The event ID
     * @param spotsAvailable Number of spots that became available
     * @return Number of entries promoted
     */
    @Transactional
    public int promoteWaitlistEntries(UUID eventId, int spotsAvailable) {
        if (spotsAvailable <= 0) {
            return 0;
        }

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        // Get waiting entries in order (FIFO)
        List<EventWaitlistEntry> waitingEntries = waitlistRepository
            .findByEventIdAndStatusOrderByCreatedAtAsc(eventId, EventWaitlistStatus.WAITING);

        if (waitingEntries.isEmpty()) {
            return 0;
        }

        int promoted = 0;
        for (EventWaitlistEntry entry : waitingEntries) {
            if (promoted >= spotsAvailable) {
                break;
            }

            try {
                // Check if event still has capacity
                Integer capacity = event.getCapacity();
                Integer currentCount = event.getCurrentAttendeeCount() != null ? event.getCurrentAttendeeCount() : 0;
                
                if (capacity == null || currentCount >= capacity) {
                    // No more capacity available
                    break;
                }

                // Promote entry
                entry.promote(null); // System promotion
                waitlistRepository.save(entry);

                // Create attendee for promoted user
                createAttendeeFromWaitlistEntry(event, entry);

                promoted++;
                log.info("Promoted waitlist entry {} for event {}", entry.getId(), eventId);
            } catch (Exception e) {
                log.error("Error promoting waitlist entry {}: {}", entry.getId(), e.getMessage(), e);
                // Continue with next entry
            }
        }

        if (promoted > 0) {
            log.info("Promoted {} waitlist entries for event {}", promoted, eventId);
        }

        return promoted;
    }

    /**
     * Create an attendee from a promoted waitlist entry.
     */
    private void createAttendeeFromWaitlistEntry(Event event, EventWaitlistEntry entry) {
        // Check if attendee already exists
        Attendee existingAttendee = null;
        if (entry.getRequester() != null) {
            Optional<Attendee> opt = attendeeRepository.findByEventIdAndUserId(
                event.getId(), entry.getRequester().getId());
            if (opt.isPresent()) {
                existingAttendee = opt.get();
            }
        }

        if (existingAttendee == null) {
            // Create new attendee
            Attendee attendee = new Attendee();
            attendee.setEvent(event);
            if (entry.getRequester() != null) {
                attendee.setUser(entry.getRequester());
            }
            attendee.setEmail(entry.getRequesterEmail());
            attendee.setName(entry.getRequesterName());
            attendee.setRsvpStatus(AttendeeStatus.CONFIRMED);
            attendee.setParticipationVisibility(VisibilityLevel.PUBLIC);
            attendeeRepository.save(attendee);

            // Update event attendee count
            Integer currentCount = event.getCurrentAttendeeCount() != null ? event.getCurrentAttendeeCount() : 0;
            event.setCurrentAttendeeCount(currentCount + 1);
            eventRepository.save(event);
        } else {
            // Update existing attendee to confirmed
            if (existingAttendee.getRsvpStatus() != AttendeeStatus.CONFIRMED) {
                existingAttendee.setRsvpStatus(AttendeeStatus.CONFIRMED);
                attendeeRepository.save(existingAttendee);

                // Update event attendee count if status changed
                Integer currentCount = event.getCurrentAttendeeCount() != null ? event.getCurrentAttendeeCount() : 0;
                event.setCurrentAttendeeCount(currentCount + 1);
                eventRepository.save(event);
            }
        }
    }

    /**
     * Get waitlist entry by ID.
     */
    @Transactional(readOnly = true)
    public EventWaitlistEntry getEntry(UUID eventId, UUID entryId) {
        return waitlistRepository.findByIdAndEventId(entryId, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found: " + entryId));
    }
}
