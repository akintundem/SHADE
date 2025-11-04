package eventplanner.security.authorization.rbac;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the mapping between roles and the permissions they grant.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RolePermissionMatrix {

    public enum RoleCategory {
        SYSTEM,
        ORGANIZATION,
        EVENT
    }

    private final EnumMap<RoleCategory, Map<String, List<String>>> rolePermissions;
    private final PermissionMatcher matcher;

    public static RolePermissionMatrix from(RbacConfig.RoleConfig config, PermissionMatcher matcher) {
        EnumMap<RoleCategory, Map<String, List<String>>> map = new EnumMap<>(RoleCategory.class);
        map.put(RoleCategory.SYSTEM, normalize(config.getSystem(), true));
        map.put(RoleCategory.ORGANIZATION, normalize(config.getOrganization(), false));
        map.put(RoleCategory.EVENT, normalize(config.getEvent(), false));
        return new RolePermissionMatrix(map, matcher);
    }

    private static Map<String, List<String>> normalize(Map<String, List<String>> source, boolean preservePrefix) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return source.entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                entry -> normalizeKey(entry.getKey(), preservePrefix),
                entry -> List.copyOf(entry.getValue())
            ));
    }

    private static String normalizeKey(String key, boolean preservePrefix) {
        if (key == null) {
            return "";
        }
        String normalized = key.trim();
        if (!preservePrefix) {
            normalized = normalized.replace("ROLE_", "");
        }
        return normalized.toUpperCase(Locale.US);
    }

    public boolean allowsSystemRole(String role, String permission) {
        return allows(RoleCategory.SYSTEM, normalizeKey(role, true), permission);
    }

    public boolean allowsOrganizationRole(String role, String permission) {
        return allows(RoleCategory.ORGANIZATION, normalizeKey(role, false), permission);
    }

    public boolean allowsEventRole(String role, String permission) {
        return allows(RoleCategory.EVENT, normalizeKey(role, false), permission);
    }

    private boolean allows(RoleCategory category, String role, String permission) {
        Map<String, List<String>> permissions = rolePermissions.getOrDefault(category, Collections.emptyMap());
        List<String> patterns = permissions.getOrDefault(role, Collections.emptyList());
        return patterns.stream().anyMatch(pattern -> matcher.matches(permission, pattern));
    }

    public boolean systemRoleHasWildcard(String role) {
        return hasWildcard(RoleCategory.SYSTEM, normalizeKey(role, true));
    }

    private boolean hasWildcard(RoleCategory category, String role) {
        Map<String, List<String>> permissions = rolePermissions.getOrDefault(category, Collections.emptyMap());
        return Optional.ofNullable(permissions.get(role))
            .orElseGet(List::of)
            .stream()
            .anyMatch("*"::equals);
    }
}
