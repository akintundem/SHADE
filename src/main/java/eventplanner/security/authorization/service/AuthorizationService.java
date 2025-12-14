package eventplanner.security.authorization.service;

import eventplanner.common.domain.enums.OrganizationRoleType;
import eventplanner.security.authorization.domain.entity.OrganizationRole;
import eventplanner.security.authorization.domain.repository.OrganizationRoleRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.authorization.domain.entity.EventRole;
import eventplanner.security.authorization.domain.repository.EventRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Central authorization service for access control.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final OrganizationRoleRepository organizationRoleRepository;
    private final EventRoleRepository eventRoleRepository;
    private final EventRepository eventRepository;

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
     * Check if a user is the owner of an organization.
     * 
     * @param user The user principal
     * @param organizationId The organization ID
     * @return true if the user is the owner of the organization
     */
    public boolean isOrganizationOwner(UserPrincipal user, UUID organizationId) {
        if (user == null || organizationId == null) {
            return false;
        }
        return isOrganizationOwner(user.getId(), organizationId);
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

    private boolean isOrganizationOwner(UUID userId, UUID organizationId) {
        if (organizationId == null) {
            return false;
        }
        return organizationRoleRepository.findByUserIdAndOrganizationIdAndActive(userId, organizationId).stream()
            .map(OrganizationRole::getRole)
            .filter(role -> role != null && !role.isBlank())
            .map(role -> role.toUpperCase(Locale.US))
            .anyMatch(role -> OrganizationRoleType.OWNER.name().equals(role));
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
     * Determine if the user holds any active organization role for the given organization.
     */
    public boolean hasOrganizationMembership(UserPrincipal user, UUID organizationId) {
        if (user == null || organizationId == null) {
            return false;
        }
        return !organizationRoleRepository.findByUserIdAndOrganizationIdAndActive(user.getId(), organizationId).isEmpty();
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
}
