package ai.eventplanner.auth.service;

import ai.eventplanner.auth.entity.OrganizationRole;
import ai.eventplanner.auth.repo.OrganizationProfileRepository;
import ai.eventplanner.auth.repo.OrganizationRoleRepository;
import ai.eventplanner.auth.security.rbac.PermissionCheck;
import ai.eventplanner.auth.security.rbac.PermissionRegistry;
import ai.eventplanner.auth.security.rbac.RolePermissionMatrix;
import ai.eventplanner.event.entity.Event;
import ai.eventplanner.event.repo.EventRepository;
import ai.eventplanner.roles.entity.EventRole;
import ai.eventplanner.roles.repo.EventRoleRepository;
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
}
