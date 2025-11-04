package eventplanner.security.authorization.service;

import eventplanner.security.authorization.domain.entity.OrganizationRole;
import eventplanner.security.auth.repository.OrganizationProfileRepository;
import eventplanner.security.authorization.domain.repository.OrganizationRoleRepository;
import eventplanner.security.authorization.rbac.PermissionCheck;
import eventplanner.security.authorization.rbac.PermissionRegistry;
import eventplanner.security.authorization.rbac.RolePermissionMatrix;
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
 * Central authorization service leveraging the RBAC registry.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final PermissionRegistry permissionRegistry;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final OrganizationProfileRepository organizationProfileRepository;
    private final EventRoleRepository eventRoleRepository;
    private final EventRepository eventRepository;

    public boolean hasPermission(UserPrincipal user, PermissionCheck check) {
        if (user == null || check == null || check.descriptor() == null) {
            return false;
        }

        String permission = check.descriptor().permission();
        if (permission == null || permission.isBlank()) {
            return true;
        }

        RolePermissionMatrix matrix = permissionRegistry.getRolePermissionMatrix();

        if (hasSystemPermission(user, permission, matrix)) {
            return true;
        }

        return switch (check.descriptor().scope()) {
            case SYSTEM -> false;
            case ORGANIZATION -> hasOrganizationPermission(user, check, matrix);
            case EVENT -> hasEventPermission(user, check, matrix);
            case PUBLIC -> true;
        };
    }

    public boolean isOwner(UserPrincipal user, PermissionCheck check) {
        if (user == null || check == null || check.descriptor() == null || !check.descriptor().allowOwner()) {
            return false;
        }

        return switch (check.descriptor().scope()) {
            case EVENT -> isEventOwner(user.getId(), check.resourceId());
            case ORGANIZATION -> isOrganizationOwner(user.getId(), check.resourceId());
            case SYSTEM -> isSelf(user.getId(), check.resourceId());
            case PUBLIC -> false;
        };
    }

    private boolean hasSystemPermission(UserPrincipal user, String permission, RolePermissionMatrix matrix) {
        return user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .anyMatch(role -> matrix.allowsSystemRole(role, permission));
    }

    private boolean hasOrganizationPermission(UserPrincipal user, PermissionCheck check, RolePermissionMatrix matrix) {
        UUID organizationId = check.resourceId();
        if (organizationId == null) {
            log.debug("Organization permission check missing resource id for user {}", user.getId());
            return false;
        }

        List<OrganizationRole> roles = organizationRoleRepository.findByUserIdAndOrganizationIdAndActive(user.getId(), organizationId);
        return roles.stream()
            .filter(OrganizationRole::getIsActive)
            .map(role -> role.getRole().toUpperCase(Locale.US))
            .anyMatch(role -> matrix.allowsOrganizationRole(role, check.descriptor().permission()));
    }

    private boolean hasEventPermission(UserPrincipal user, PermissionCheck check, RolePermissionMatrix matrix) {
        UUID eventId = check.resourceId();
        if (eventId == null) {
            log.debug("Event permission check missing resource id for user {}", user.getId());
            return false;
        }

        List<EventRole> eventRoles = eventRoleRepository.findByEventIdAndUserId(eventId, user.getId());
        return eventRoles.stream()
            .filter(EventRole::getIsActive)
            .map(role -> role.getRoleName().name().toUpperCase(Locale.US))
            .anyMatch(role -> matrix.allowsEventRole(role, check.descriptor().permission()));
    }

    private boolean isEventOwner(UUID userId, UUID eventId) {
        if (eventId == null) {
            return false;
        }
        return eventRepository.findById(eventId)
            .map(Event::getOwnerId)
            .filter(userId::equals)
            .isPresent();
    }

    private boolean isOrganizationOwner(UUID userId, UUID organizationId) {
        if (organizationId == null) {
            return false;
        }
        return organizationProfileRepository.findById(organizationId)
            .map(profile -> profile.getOwner() != null && userId.equals(profile.getOwner().getId()))
            .orElse(false);
    }

    private boolean isSelf(UUID userId, UUID targetId) {
        return targetId != null && targetId.equals(userId);
    }

    /**
     * Check if a user can access an event.
     * A user can access an event if:
     * 1. The event is public, OR
     * 2. The user is the owner, OR
     * 3. The user has an active role in the event
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
        
        if (hasActiveRole) {
            return true;
        }

        // System admins can access all events
        RolePermissionMatrix matrix = permissionRegistry.getRolePermissionMatrix();
        if (hasSystemPermission(user, "event.*", matrix) || hasSystemPermission(user, "event.read", matrix)) {
            return true;
        }

        return false;
    }
}
