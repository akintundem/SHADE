package eventplanner.security.authorization.rbac.model;

import eventplanner.security.filters.RbacContextFilter;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Thread scoped authorization context prepared by {@link RbacContextFilter}.
 */
@Value
@Builder
public class RbacRequestContext {
    UUID userId;
    Set<String> systemRoles;
    Map<UUID, Set<String>> eventRoles;

    public Set<String> getSystemRoles() {
        return systemRoles == null ? Set.of() : systemRoles;
    }

    public Map<UUID, Set<String>> getEventRoles() {
        return eventRoles == null ? Map.of() : eventRoles;
    }

    public Set<String> getEventRoles(UUID eventId) {
        if (eventId == null || eventRoles == null) {
            return Set.of();
        }
        return eventRoles.getOrDefault(eventId, Set.of());
    }
}
