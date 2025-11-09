package eventplanner.security.authorization.rbac;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Thread scoped authorization context prepared by {@link eventplanner.security.filters.RbacContextFilter}.
 */
@Value
@Builder
public class RbacRequestContext {
    UUID userId;
    Set<String> systemRoles;
    Map<UUID, Set<String>> eventRoles;
    Map<UUID, Set<String>> organizationRoles;

    public Set<String> getSystemRoles() {
        return systemRoles == null ? Set.of() : systemRoles;
    }

    public Map<UUID, Set<String>> getEventRoles() {
        return eventRoles == null ? Map.of() : eventRoles;
    }

    public Map<UUID, Set<String>> getOrganizationRoles() {
        return organizationRoles == null ? Map.of() : organizationRoles;
    }

    public Set<String> getEventRoles(UUID eventId) {
        if (eventId == null || eventRoles == null) {
            return Set.of();
        }
        return eventRoles.getOrDefault(eventId, Set.of());
    }

    public Set<String> getOrganizationRoles(UUID organizationId) {
        if (organizationId == null || organizationRoles == null) {
            return Set.of();
        }
        return organizationRoles.getOrDefault(organizationId, Set.of());
    }
}
