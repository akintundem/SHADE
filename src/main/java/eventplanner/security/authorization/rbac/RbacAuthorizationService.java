package eventplanner.security.authorization.rbac;

import eventplanner.features.timeline.repository.TimelineItemRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Evaluates {@link RequiresPermission} declarations against the loaded policy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RbacAuthorizationService {

    private final RbacPolicyStore policyStore;
    private final AuthorizationService authorizationService;
    private final TimelineItemRepository timelineItemRepository;

    public void assertAuthorized(UserPrincipal principal, String permissionName, Map<String, Object> resources) {
        if (!isAuthorized(principal, permissionName, resources)) {
            throw new AccessDeniedException("Access denied for permission " + permissionName);
        }
    }

    public boolean isAuthorized(UserPrincipal principal, String permissionName, Map<String, Object> resources) {
        var permission = policyStore.findPermission(permissionName)
                .orElseThrow(() -> new AccessDeniedException("Permission not defined in policy: " + permissionName));

        var abac = permission.getAbac();
        if (abac.isAuthenticated() && principal == null) {
            return false;
        }

        RbacScope scope = permission.getScope();
        Map<String, Object> resourceValues = resources == null ? Map.of() : resources;

        if (!isRoleGranted(principal, scope, permission.getName(), resourceValues)) {
            log.debug("RBAC denied - role grant missing for permission {}", permissionName);
            return false;
        }

        if (abac.isOwnsScope() && !ownsScope(principal, scope, resourceValues)) {
            log.debug("RBAC denied - ownership check failed for permission {}", permissionName);
            return false;
        }

        if (abac.isMember() && !isMember(principal, scope, resourceValues)) {
            log.debug("RBAC denied - membership check failed for permission {}", permissionName);
            return false;
        }

        if (!permission.getConditions().isEmpty()) {
            log.debug("RBAC manual conditions for {} -> {}", permissionName, permission.getConditions());
        }
        return true;
    }

    private boolean isRoleGranted(UserPrincipal principal, RbacScope scope, String permissionName, Map<String, Object> resources) {
        Set<String> userRoles = resolveRoles(principal, scope, resources);
        return policyStore.isPermissionGranted(scope, userRoles, permissionName);
    }

    private Set<String> resolveRoles(UserPrincipal principal, RbacScope scope, Map<String, Object> resources) {
        RbacRequestContext context = RbacRequestContextHolder.get();
        Set<String> roles = new LinkedHashSet<>();

        if (scope == RbacScope.PUBLIC) {
            roles.add("PUBLIC");
            return roles;
        }

        switch (scope) {
            case SYSTEM -> {
                if (context != null) {
                    roles.addAll(context.getSystemRoles());
                } else {
                    roles.add("USER");
                }
            }
            case EVENT -> {
                UUID eventId = extractUuid(resources, "event_id");
                // If event_id is missing, try to resolve it from task_id
                if (eventId == null) {
                    UUID taskId = extractUuid(resources, "task_id");
                    if (taskId != null) {
                        eventId = resolveEventIdFromTaskId(taskId);
                        if (eventId != null) {
                            resources.put("event_id", eventId);
                        }
                    }
                }
                roles.addAll(resolveEventRoles(context, resources));
                // Always check if user is event owner and add ORGANIZER role
                // This ensures event owners have access even if context doesn't have the role yet
                if (principal != null && eventId != null) {
                    if (authorizationService.isEventOwner(principal, eventId)) {
                        roles.add("ORGANIZER");
                    }
                }
            }
            case ORGANIZATION -> {
                roles.addAll(resolveOrganizationRoles(context, resources));
                if (roles.isEmpty() && principal != null) {
                    UUID orgId = extractUuid(resources, "organization_id");
                    if (orgId != null && authorizationService.isOrganizationOwner(principal, orgId)) {
                        roles.add("OWNER");
                    }
                }
            }
            default -> {
            }
        }
        return roles;
    }

    private Collection<String> resolveEventRoles(RbacRequestContext context, Map<String, Object> resources) {
        if (context == null) {
            return Set.of();
        }
        UUID eventId = extractUuid(resources, "event_id");
        if (eventId == null) {
            return Set.of();
        }
        return context.getEventRoles(eventId);
    }

    private Collection<String> resolveOrganizationRoles(RbacRequestContext context, Map<String, Object> resources) {
        if (context == null) {
            return Set.of();
        }
        UUID organizationId = extractUuid(resources, "organization_id");
        if (organizationId == null) {
            return Set.of();
        }
        return context.getOrganizationRoles(organizationId);
    }

    private boolean ownsScope(UserPrincipal principal, RbacScope scope, Map<String, Object> resources) {
        if (principal == null) {
            return false;
        }
        return switch (scope) {
            case SYSTEM -> {
                UUID targetUser = Optional.ofNullable(extractUuid(resources, "user_id"))
                        .orElseGet(() -> extractUuid(resources, "target_user_id"));
                yield targetUser != null && targetUser.equals(principal.getId());
            }
            case EVENT -> {
                UUID eventId = extractUuid(resources, "event_id");
                // If event_id is missing, try to resolve it from task_id
                if (eventId == null) {
                    UUID taskId = extractUuid(resources, "task_id");
                    if (taskId != null) {
                        eventId = resolveEventIdFromTaskId(taskId);
                        if (eventId != null) {
                            resources.put("event_id", eventId);
                        }
                    }
                }
                yield eventId != null && authorizationService.isEventOwner(principal, eventId);
            }
            case ORGANIZATION -> {
                UUID orgId = extractUuid(resources, "organization_id");
                yield orgId != null && authorizationService.isOrganizationOwner(principal, orgId);
            }
            case PUBLIC -> true;
        };
    }

    private boolean isMember(UserPrincipal principal, RbacScope scope, Map<String, Object> resources) {
        if (principal == null) {
            return false;
        }
        RbacRequestContext context = RbacRequestContextHolder.get();
        return switch (scope) {
            case SYSTEM -> true;
            case EVENT -> {
                UUID eventId = extractUuid(resources, "event_id");
                // If event_id is missing, try to resolve it from task_id
                if (eventId == null) {
                    UUID taskId = extractUuid(resources, "task_id");
                    if (taskId != null) {
                        eventId = resolveEventIdFromTaskId(taskId);
                    }
                }
                if (eventId == null) {
                    yield false;
                }
                // Update resources with resolved event_id for subsequent checks
                if (eventId != null && !resources.containsKey("event_id")) {
                    resources.put("event_id", eventId);
                }
                if (!resolveEventRoles(context, resources).isEmpty()) {
                    yield true;
                }
                // Event owner is automatically a member
                if (authorizationService.isEventOwner(principal, eventId)) {
                    yield true;
                }
                yield authorizationService.hasEventMembership(principal, eventId);
            }
            case ORGANIZATION -> {
                UUID orgId = extractUuid(resources, "organization_id");
                if (orgId == null) {
                    yield false;
                }
                if (!resolveOrganizationRoles(context, resources).isEmpty()) {
                    yield true;
                }
                yield authorizationService.hasOrganizationMembership(principal, orgId);
            }
            case PUBLIC -> true;
        };
    }

    private UUID extractUuid(Map<String, Object> resources, String key) {
        if (resources == null || !resources.containsKey(key)) {
            return null;
        }
        Object value = resources.get(key);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return UUID.fromString(str.trim());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid %s format".formatted(key.replace('_', ' '))
                );
            }
        }
        return null;
    }

    /**
     * Resolve event_id from task_id by looking up the timeline item
     */
    private UUID resolveEventIdFromTaskId(UUID taskId) {
        if (taskId == null) {
            return null;
        }
        try {
            return timelineItemRepository.findById(taskId)
                    .map(item -> item.getEvent() != null ? item.getEvent().getId() : null)
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Failed to resolve event_id from task_id {}: {}", taskId, e.getMessage());
            return null;
        }
    }
}
