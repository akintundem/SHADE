package eventplanner.common.communication.services.core;

import eventplanner.common.communication.services.core.dto.NotificationTarget;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to resolve notification targets for various recipient groups.
 * Supports flexible targeting for bulk notifications.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationTargetResolver {

    private final UserAccountRepository userAccountRepository;
    private final AttendeeRepository attendeeRepository;
    private final EventUserRepository eventUserRepository;

    /**
     * Resolve notification targets based on target type and parameters
     */
    public NotificationTarget resolveTarget(NotificationTarget.TargetType targetType, UUID eventId, Map<String, Object> parameters) {
        switch (targetType) {
            case ALL_USERS:
                return resolveAllUsers(parameters);
            case EVENT_ATTENDEES:
                return resolveEventAttendees(eventId, parameters);
            case EVENT_GUESTS:
                return resolveEventGuests(eventId, parameters);
            case EVENT_COLLABORATORS:
                return resolveEventCollaborators(eventId, parameters);
            case EVENT_CHECKED_IN:
                return resolveEventCheckedIn(eventId);
            case EVENT_NOT_CHECKED_IN:
                return resolveEventNotCheckedIn(eventId);
            case SPECIFIC_USERS:
                return resolveSpecificUsers(parameters);
            case USER_SEGMENT:
                return resolveUserSegment(parameters);
            default:
                throw new IllegalArgumentException("Unsupported target type: " + targetType);
        }
    }

    /**
     * Resolve all users in the system (with pagination support)
     */
    private NotificationTarget resolveAllUsers(Map<String, Object> parameters) {
        Set<UUID> userIds = new HashSet<>();
        
        // Support pagination for large user bases
        int page = parameters != null && parameters.containsKey("page") 
                ? (Integer) parameters.get("page") : 0;
        int size = parameters != null && parameters.containsKey("size") 
                ? (Integer) parameters.get("size") : 1000;
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserAccount> userPage = userAccountRepository.findAll(pageable);
        
        while (userPage.hasContent()) {
            userIds.addAll(userPage.getContent().stream()
                    .map(UserAccount::getId)
                    .collect(Collectors.toSet()));
            
            if (!userPage.hasNext()) {
                break;
            }
            pageable = userPage.nextPageable();
            userPage = userAccountRepository.findAll(pageable);
        }
        
        log.info("Resolved {} users for ALL_USERS target", userIds.size());
        return new NotificationTarget(NotificationTarget.TargetType.ALL_USERS, userIds, Collections.emptySet());
    }

    /**
     * Resolve all attendees for an event (including those with user accounts)
     */
    private NotificationTarget resolveEventAttendees(UUID eventId, Map<String, Object> parameters) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required for EVENT_ATTENDEES target");
        }
        
        List<Attendee> attendees = attendeeRepository.findByEventId(eventId);
        
        Set<UUID> userIds = attendees.stream()
                .map(Attendee::getUser)
                .filter(Objects::nonNull)
                .map(UserAccount::getId)
                .collect(Collectors.toSet());
        
        Set<String> emails = attendees.stream()
                .map(Attendee::getEmail)
                .filter(Objects::nonNull)
                .filter(email -> !email.isBlank())
                .collect(Collectors.toSet());
        
        // Filter by RSVP status if provided
        if (parameters != null && parameters.containsKey("rsvpStatus")) {
            AttendeeStatus status = (AttendeeStatus) parameters.get("rsvpStatus");
            attendees = attendees.stream()
                    .filter(a -> a.getRsvpStatus() == status)
                    .collect(Collectors.toList());
            
            userIds = attendees.stream()
                    .map(Attendee::getUser)
                    .filter(Objects::nonNull)
                    .map(UserAccount::getId)
                    .collect(Collectors.toSet());
            
            emails = attendees.stream()
                    .map(Attendee::getEmail)
                    .filter(Objects::nonNull)
                    .filter(email -> !email.isBlank())
                    .collect(Collectors.toSet());
        }
        
        log.info("Resolved {} user IDs and {} emails for EVENT_ATTENDEES target (event: {})", 
                userIds.size(), emails.size(), eventId);
        return new NotificationTarget(NotificationTarget.TargetType.EVENT_ATTENDEES, userIds, emails);
    }

    /**
     * Resolve all guests (external attendees without user accounts)
     */
    private NotificationTarget resolveEventGuests(UUID eventId, Map<String, Object> parameters) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required for EVENT_GUESTS target");
        }
        
        List<Attendee> attendees = attendeeRepository.findByEventId(eventId);
        
        // Only attendees without linked user accounts
        Set<String> emails = attendees.stream()
                .filter(a -> a.getUser() == null)
                .map(Attendee::getEmail)
                .filter(Objects::nonNull)
                .filter(email -> !email.isBlank())
                .collect(Collectors.toSet());
        
        log.info("Resolved {} guest emails for EVENT_GUESTS target (event: {})", emails.size(), eventId);
        return new NotificationTarget(NotificationTarget.TargetType.EVENT_GUESTS, Collections.emptySet(), emails);
    }

    /**
     * Resolve all collaborators for an event
     */
    private NotificationTarget resolveEventCollaborators(UUID eventId, Map<String, Object> parameters) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required for EVENT_COLLABORATORS target");
        }
        
        List<EventUser> collaborators = eventUserRepository.findByEventId(eventId);
        
        Set<UUID> userIds = collaborators.stream()
                .map(EventUser::getUser)
                .filter(Objects::nonNull)
                .map(UserAccount::getId)
                .collect(Collectors.toSet());
        
        Set<String> emails = collaborators.stream()
                .map(EventUser::getUser)
                .filter(Objects::nonNull)
                .map(UserAccount::getEmail)
                .filter(Objects::nonNull)
                .filter(email -> !email.isBlank())
                .collect(Collectors.toSet());
        
        log.info("Resolved {} collaborators for EVENT_COLLABORATORS target (event: {})", userIds.size(), eventId);
        return new NotificationTarget(NotificationTarget.TargetType.EVENT_COLLABORATORS, userIds, emails);
    }

    /**
     * Resolve attendees who have checked in
     */
    private NotificationTarget resolveEventCheckedIn(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required for EVENT_CHECKED_IN target");
        }
        
        Page<Attendee> checkedInAttendees = attendeeRepository.findCheckedInByEventId(
                eventId, Pageable.unpaged());
        
        Set<UUID> userIds = checkedInAttendees.getContent().stream()
                .map(Attendee::getUser)
                .filter(Objects::nonNull)
                .map(UserAccount::getId)
                .collect(Collectors.toSet());
        
        Set<String> emails = checkedInAttendees.getContent().stream()
                .map(Attendee::getEmail)
                .filter(Objects::nonNull)
                .filter(email -> !email.isBlank())
                .collect(Collectors.toSet());
        
        log.info("Resolved {} checked-in attendees for EVENT_CHECKED_IN target (event: {})", 
                userIds.size() + emails.size(), eventId);
        return new NotificationTarget(NotificationTarget.TargetType.EVENT_CHECKED_IN, userIds, emails);
    }

    /**
     * Resolve attendees who have not checked in
     */
    private NotificationTarget resolveEventNotCheckedIn(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required for EVENT_NOT_CHECKED_IN target");
        }
        
        Page<Attendee> notCheckedInAttendees = attendeeRepository.findNotCheckedInByEventId(
                eventId, Pageable.unpaged());
        
        Set<UUID> userIds = notCheckedInAttendees.getContent().stream()
                .map(Attendee::getUser)
                .filter(Objects::nonNull)
                .map(UserAccount::getId)
                .collect(Collectors.toSet());
        
        Set<String> emails = notCheckedInAttendees.getContent().stream()
                .map(Attendee::getEmail)
                .filter(Objects::nonNull)
                .filter(email -> !email.isBlank())
                .collect(Collectors.toSet());
        
        log.info("Resolved {} not-checked-in attendees for EVENT_NOT_CHECKED_IN target (event: {})", 
                userIds.size() + emails.size(), eventId);
        return new NotificationTarget(NotificationTarget.TargetType.EVENT_NOT_CHECKED_IN, userIds, emails);
    }

    /**
     * Resolve specific users by their IDs
     */
    @SuppressWarnings("unchecked")
    private NotificationTarget resolveSpecificUsers(Map<String, Object> parameters) {
        if (parameters == null || !parameters.containsKey("userIds")) {
            throw new IllegalArgumentException("userIds parameter is required for SPECIFIC_USERS target");
        }
        
        List<UUID> userIdsList = (List<UUID>) parameters.get("userIds");
        if (userIdsList == null || userIdsList.isEmpty()) {
            return new NotificationTarget(NotificationTarget.TargetType.SPECIFIC_USERS, 
                    Collections.emptySet(), Collections.emptySet());
        }
        
        Set<UUID> userIds = new HashSet<>(userIdsList);
        
        // Optionally fetch emails for these users
        Set<String> emails = new HashSet<>();
        for (UUID userId : userIds) {
            userAccountRepository.findById(userId)
                    .map(UserAccount::getEmail)
                    .filter(Objects::nonNull)
                    .filter(email -> !email.isBlank())
                    .ifPresent(emails::add);
        }
        
        log.info("Resolved {} specific users for SPECIFIC_USERS target", userIds.size());
        return new NotificationTarget(NotificationTarget.TargetType.SPECIFIC_USERS, userIds, emails);
    }

    /**
     * Resolve users by segment (e.g., by user type, status, etc.)
     */
    private NotificationTarget resolveUserSegment(Map<String, Object> parameters) {
        // This is a placeholder for future segment-based targeting
        // Could filter by userType, status, registration date, etc.
        log.warn("USER_SEGMENT targeting not yet fully implemented");
        return new NotificationTarget(NotificationTarget.TargetType.USER_SEGMENT, 
                Collections.emptySet(), Collections.emptySet());
    }
}

