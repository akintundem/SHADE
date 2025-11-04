package eventplanner.security.authorization.rbac;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiled representation of a single route definition.
 */
@Getter
@Slf4j
public class RoutePermission {

    private final String name;
    private final Pattern pattern;
    private final Map<String, PermissionDescriptor> methodPermissions;
    private final String resourceIdGroup;

    private RoutePermission(String name,
                            Pattern pattern,
                            Map<String, PermissionDescriptor> methodPermissions,
                            String resourceIdGroup) {
        this.name = name;
        this.pattern = pattern;
        this.methodPermissions = Map.copyOf(methodPermissions);
        this.resourceIdGroup = resourceIdGroup;
    }

    public static RoutePermission from(RbacConfig.RouteConfig config) {
        if (config.getPattern() == null || config.getPattern().isBlank()) {
            throw new IllegalArgumentException("RBAC route pattern must not be blank");
        }

        Pattern compiledPattern = Pattern.compile(config.getPattern());

        Map<String, PermissionDescriptor> methods = config.getMethods().entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                entry -> entry.getKey().toUpperCase(Locale.US),
                entry -> PermissionDescriptor.builder()
                    .permission(entry.getValue().getPermission())
                    .scope(entry.getValue().getScope() != null ? entry.getValue().getScope() : AccessScope.SYSTEM)
                    .allowOwner(entry.getValue().isAllowOwner())
                    .allowAuthenticated(entry.getValue().isAllowAuthenticated())
                    .build()
            ));

        return new RoutePermission(
            config.getName(),
            compiledPattern,
            methods,
            config.getResourceIdGroup()
        );
    }

    public Optional<PermissionCheck> resolve(HttpServletRequest request) {
        Matcher matcher = pattern.matcher(request.getRequestURI());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        PermissionDescriptor descriptor = methodPermissions.get(request.getMethod().toUpperCase(Locale.US));
        if (descriptor == null) {
            return Optional.empty();
        }

        String resourceValue = extractGroup(matcher);
        UUID resourceId = parseUuid(resourceValue);
        return Optional.of(new PermissionCheck(descriptor, resourceId, resourceValue));
    }

    private String extractGroup(Matcher matcher) {
        if (resourceIdGroup == null || resourceIdGroup.isBlank()) {
            return null;
        }

        try {
            if (resourceIdGroup.chars().allMatch(Character::isDigit)) {
                int index = Integer.parseInt(resourceIdGroup, 10);
                return matcher.group(index);
            }
            return matcher.group(resourceIdGroup);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.debug("Unable to extract resource id group '{}' for route '{}'", resourceIdGroup, name);
            return null;
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            log.debug("Extracted resource id '{}' is not a valid UUID for route '{}'", value, name);
            return null;
        }
    }
}
