package eventplanner.security.authorization.service;

import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;
import eventplanner.features.attendee.repository.AttendeeInviteRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.event.entity.EventRole;
import eventplanner.features.event.repository.EventRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Central authorization service for access control.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final EventRoleRepository eventRoleRepository;
    private final EventRepository eventRepository;
    private final AttendeeInviteRepository attendeeInviteRepository;

    /**
     * Check if a user is the owner of an event.
     * 
     * @param user The user principal
     * @param eventId The event ID
     * @return true if the user is the owner of the event
     */
    public boolean isEventOwner(UserPrincipal user, UUID eventId) {
        if (user == null || eventId == null) {
            return false;
        }
        return isEventOwner(user.getId(), eventId);
    }

    /**
     * Check if a user is accessing their own resource.
     * 
     * @param user The user principal
     * @param targetId The target resource ID
     * @return true if the user ID matches the target ID
     */
    public boolean isSelf(UserPrincipal user, UUID targetId) {
        if (user == null || targetId == null) {
            return false;
        }
        return user.getId().equals(targetId);
    }

    /**
     * Check if a user has an admin role.
     * 
     * @param user The user principal
     * @return true if the user has ROLE_ADMIN authority
     */
    public boolean isAdmin(UserPrincipal user) {
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority));
    }

    private boolean isEventOwner(UUID userId, UUID eventId) {
        if (eventId == null) {
            return false;
        }
        return eventRepository.findById(eventId)
            .map(event -> event.getOwner() != null ? event.getOwner().getId() : null)
            .filter(userId::equals)
            .isPresent();
    }

    /**
     * Check if a user can access an event.
     * A user can access an event if:
     * 1. The event is public, OR
     * 2. The user is the owner, OR
     * 3. The user has an active role in the event, OR
     * 4. The user is an admin
     * 
     * @param user The user principal
     * @param eventId The event ID
     * @return true if the user can access the event
     */
    public boolean canAccessEvent(UserPrincipal user, UUID eventId) {
        if (user == null || eventId == null) {
            return false;
        }

        // Check if event exists
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return false;
        }

        // System admins can access all events
        if (isAdmin(user)) {
            return true;
        }

        // Public events are accessible to everyone
        if (Boolean.TRUE.equals(event.getIsPublic())) {
            return true;
        }

        // Private events: check ownership
        if (isEventOwner(user.getId(), eventId)) {
            return true;
        }

        // Private events: check if user has an active role
        List<EventRole> eventRoles = eventRoleRepository.findByEventIdAndUserId(eventId, user.getId());
        boolean hasActiveRole = eventRoles.stream()
            .anyMatch(EventRole::getIsActive);
        
        return hasActiveRole;
    }

    /**
     * Check if a user can access a private event when accepted invites should be honored.
     * This is intended for read-only flows such as ticket type listing or ticket issuance.
     */
    public boolean canAccessEventWithInvite(UserPrincipal user, UUID eventId) {
        if (eventId == null) {
            return false;
        }
        Event event = eventRepository.findById(eventId).orElse(null);
        return canAccessEventWithInvite(user, event);
    }

    public boolean canAccessEventWithInvite(UserPrincipal user, Event event) {
        if (event == null) {
            return false;
        }
        if (Boolean.TRUE.equals(event.getIsPublic())) {
            return true;
        }
        if (user == null) {
            return false;
        }
        UUID eventId = event.getId();
        if (isAdmin(user) || isEventOwner(user.getId(), eventId)) {
            return true;
        }
        if (hasEventMembership(user, eventId)) {
            return true;
        }
        return hasAcceptedInvite(user, eventId);
    }

    /**
     * Determine if the user has an active event role for the provided event.
     */
    public boolean hasEventMembership(UserPrincipal user, UUID eventId) {
        if (user == null || eventId == null) {
            return false;
        }
        return eventRoleRepository.findByEventIdAndUserId(eventId, user.getId()).stream()
            .anyMatch(EventRole::getIsActive);
    }

    /**
     * Check if a user can manage an event (admin, owner, or active member).
     * Consolidates the identical checks that were duplicated in AttendeeController,
     * AttendeeService, TicketApprovalService, and TicketWaitlistService.
     */
    public boolean canManageEvent(UserPrincipal user, UUID eventId) {
        if (user == null || eventId == null) {
            return false;
        }
        return isAdmin(user) || isEventOwner(user, eventId) || hasEventMembership(user, eventId);
    }

    private boolean hasAcceptedInvite(UserPrincipal user, UUID eventId) {
        if (user == null || eventId == null) {
            return false;
        }
        UUID userId = user.getId();
        if (userId != null && attendeeInviteRepository.existsByEventIdAndInviteeIdAndStatus(
                eventId, userId, AttendeeInviteStatus.ACCEPTED)) {
            return true;
        }
        String email = user.getUser() != null ? user.getUser().getEmail() : null;
        return email != null && attendeeInviteRepository.existsByEventIdAndInviteeEmailIgnoreCaseAndStatus(
            eventId, email, AttendeeInviteStatus.ACCEPTED);
    }
}
