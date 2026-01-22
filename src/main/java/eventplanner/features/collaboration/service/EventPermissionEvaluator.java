package eventplanner.features.collaboration.service;

import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.entity.EventUserPermission;
import eventplanner.features.collaboration.enums.EventPermission;
import eventplanner.features.collaboration.util.EventPermissionDefaults;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Evaluates effective permissions for an event collaborator.
 */
@Component
public class EventPermissionEvaluator {

    public boolean hasPermission(EventUser membership, EventPermission permission) {
        if (permission == null) {
            return false;
        }
        return resolveEffectivePermissions(membership).contains(permission);
    }

    public boolean hasAnyPermission(EventUser membership, EventPermission... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        Set<EventPermission> effectivePermissions = resolveEffectivePermissions(membership);
        for (EventPermission permission : permissions) {
            if (permission != null && effectivePermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    private Set<EventPermission> resolveEffectivePermissions(EventUser membership) {
        if (membership == null) {
            return EnumSet.noneOf(EventPermission.class);
        }
        List<EventUserPermission> overrides = membership.getPermissions();
        if (overrides != null && !overrides.isEmpty()) {
            EnumSet<EventPermission> explicit = EnumSet.noneOf(EventPermission.class);
            for (EventUserPermission override : overrides) {
                if (override != null && override.getPermission() != null) {
                    explicit.add(override.getPermission());
                }
            }
            return explicit;
        }
        return EventPermissionDefaults.defaultsForRole(membership.getUserType());
    }
}
