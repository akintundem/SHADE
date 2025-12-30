package eventplanner.security.authorization.rbac;

import eventplanner.features.attendee.repository.AttendeeInviteRepository;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.features.timeline.repository.TaskRepository;
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
    private final TaskRepository taskRepository;
    private final AttendeeRepository attendeeRepository;
    private final AttendeeInviteRepository attendeeInviteRepository;
    private final TicketRepository ticketRepository;

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
                // If event_id is missing, try to resolve it from task_id, attendance_id, or invite_id
                if (eventId == null) {
                    UUID taskId = extractUuid(resources, "task_id");
                    if (taskId != null) {
                        eventId = resolveEventIdFromTaskId(taskId);
                        if (eventId != null) {
                            resources.put("event_id", eventId);
                        }
                    }
                }
                if (eventId == null) {
                    UUID attendanceId = extractUuid(resources, "attendance_id");
                    if (attendanceId != null) {
                        eventId = resolveEventIdFromAttendanceId(attendanceId);
                        if (eventId != null) {
                            resources.put("event_id", eventId);
                        }
                    }
                }
                if (eventId == null) {
                    UUID inviteId = extractUuid(resources, "invite_id");
                    if (inviteId != null) {
                        eventId = resolveEventIdFromInviteId(inviteId);
                        if (eventId != null) {
                            resources.put("event_id", eventId);
                        }
                    }
                }
                if (eventId == null) {
                    UUID ticketId = extractUuid(resources, "ticket_id");
                    if (ticketId != null) {
                        eventId = resolveEventIdFromTicketId(ticketId);
                        if (eventId != null) {
                            resources.put("event_id", eventId);
                        }
                    }
                }
                // If we still can't resolve event_id, we can't determine roles
                // Allow the request to proceed to validation which will catch the missing parameters
                if (eventId == null) {
                    // Return empty roles - this will cause authorization to fail
                    // but allows validation to run first and return 400 instead of 403
                    return roles;
                }
                roles.addAll(resolveEventRoles(context, resources));
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
                // If event_id is missing, try to resolve it from task_id or attendance_id
                if (eventId == null) {
                    UUID taskId = extractUuid(resources, "task_id");
                    if (taskId != null) {
                        eventId = resolveEventIdFromTaskId(taskId);
                        if (eventId != null) {
                            resources.put("event_id", eventId);
                        }
                    }
                }
                if (eventId == null) {
                    UUID attendanceId = extractUuid(resources, "attendance_id");
                    if (attendanceId != null) {
                        eventId = resolveEventIdFromAttendanceId(attendanceId);
                        if (eventId != null) {
                            resources.put("event_id", eventId);
                        }
                    }
                }
                if (eventId == null) {
                    UUID inviteId = extractUuid(resources, "invite_id");
                    if (inviteId != null) {
                        eventId = resolveEventIdFromInviteId(inviteId);
                        if (eventId != null) {
                            resources.put("event_id", eventId);
                        }
                    }
                }
                if (eventId == null) {
                    UUID ticketId = extractUuid(resources, "ticket_id");
                    if (ticketId != null) {
                        eventId = resolveEventIdFromTicketId(ticketId);
                        if (eventId != null) {
                            resources.put("event_id", eventId);
                        }
                    }
                }
                if (eventId == null) {
                    UUID ticketId = extractUuid(resources, "ticket_id");
                    if (ticketId != null) {
                        eventId = resolveEventIdFromTicketId(ticketId);
                        if (eventId != null) {
                            resources.put("event_id", eventId);
                        }
                    }
                }
                yield eventId != null && authorizationService.isEventOwner(principal, eventId);
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
                // If event_id is missing, try to resolve it from task_id or attendance_id
                if (eventId == null) {
                    UUID taskId = extractUuid(resources, "task_id");
                    if (taskId != null) {
                        eventId = resolveEventIdFromTaskId(taskId);
                    }
                }
                if (eventId == null) {
                    UUID attendanceId = extractUuid(resources, "attendance_id");
                    if (attendanceId != null) {
                        eventId = resolveEventIdFromAttendanceId(attendanceId);
                    }
                }
                if (eventId == null) {
                    UUID inviteId = extractUuid(resources, "invite_id");
                    if (inviteId != null) {
                        eventId = resolveEventIdFromInviteId(inviteId);
                    }
                }
                if (eventId == null) {
                    UUID ticketId = extractUuid(resources, "ticket_id");
                    if (ticketId != null) {
                        eventId = resolveEventIdFromTicketId(ticketId);
                    }
                }
                // If we can't resolve event_id, we cannot verify membership
                // The role check will also fail (returns empty roles), so authorization will be denied
                // This allows validation to catch missing parameters and return 400 instead of 403
                // However, we must deny membership check to maintain security
                if (eventId == null) {
                    yield false;  // Deny by default - security first
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
            return taskRepository.findById(taskId)
                    .map(item -> item.getEvent() != null ? item.getEvent().getId() : null)
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Failed to resolve event_id from task_id {}: {}", taskId, e.getMessage());
            return null;
        }
    }

    /**
     * Resolve event_id from attendance_id by looking up the attendee
     */
    private UUID resolveEventIdFromAttendanceId(UUID attendanceId) {
        if (attendanceId == null) {
            return null;
        }
        try {
            return attendeeRepository.findById(attendanceId)
                    .map(attendee -> attendee.getEvent() != null ? attendee.getEvent().getId() : null)
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Failed to resolve event_id from attendance_id {}: {}", attendanceId, e.getMessage());
            return null;
        }
    }

    /**
     * Resolve event_id from invite_id by looking up the attendee invite
     */
    private UUID resolveEventIdFromInviteId(UUID inviteId) {
        if (inviteId == null) {
            return null;
        }
        try {
            return attendeeInviteRepository.findById(inviteId)
                    .map(invite -> invite.getEvent() != null ? invite.getEvent().getId() : null)
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Failed to resolve event_id from invite_id {}: {}", inviteId, e.getMessage());
            return null;
        }
    }

    /**
     * Resolve event_id from ticket_id by looking up the ticket
     */
    private UUID resolveEventIdFromTicketId(UUID ticketId) {
        if (ticketId == null) {
            return null;
        }
        try {
            return ticketRepository.findById(ticketId)
                    .map(ticket -> ticket.getEvent() != null ? ticket.getEvent().getId() : null)
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Failed to resolve event_id from ticket_id {}: {}", ticketId, e.getMessage());
            return null;
        }
    }
}
