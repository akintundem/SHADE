package eventplanner.security.authorization.rbac.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import eventplanner.security.authorization.rbac.model.RbacPermissionDefinition;
import eventplanner.security.authorization.rbac.model.RbacPolicyMetadata;
import eventplanner.security.authorization.rbac.model.RbacScope;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Loads RBAC_policy.yml into memory and exposes fast lookup helpers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RbacPolicyStore {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    @Value("${rbac.policy.location:classpath:rbac/RBAC_policy.yml}")
    private String policyLocation;

    @Getter
    private RbacPolicyMetadata metadata;

    private final Map<String, RbacPermissionDefinition> permissionIndex = new ConcurrentHashMap<>();
    private final Map<RbacScope, Map<String, Set<String>>> roleIndex = new EnumMap<>(RbacScope.class);

    @PostConstruct
    public void loadPolicy() {
        try {
            Resource resource = resourceLoader.getResource(policyLocation);
            if (!resource.exists()) {
                throw new IllegalStateException("RBAC policy not found at " + policyLocation);
            }
            try (InputStream in = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(in);
                JsonNode policyNode = root.path("policy");
                metadata = RbacPolicyMetadata.builder()
                        .name(policyNode.path("name").asText("undefined"))
                        .version(policyNode.path("version").asText("unknown"))
                        .source(policyNode.path("source").asText("unknown"))
                        .build();

                permissionIndex.clear();
                roleIndex.clear();

                parsePermissions(policyNode.path("permissions"));
                parseRoles(policyNode.path("roles"));

                log.info("Loaded RBAC policy '{}' (version {}) with {} permissions across {} scopes",
                        metadata.getName(),
                        metadata.getVersion(),
                        permissionIndex.size(),
                        roleIndex.size());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read RBAC policy at " + policyLocation, ex);
        }
    }

    public Optional<RbacPermissionDefinition> findPermission(String permissionName) {
        if (!StringUtils.hasText(permissionName)) {
            return Optional.empty();
        }
        return Optional.ofNullable(permissionIndex.get(normalizePermission(permissionName)));
    }

    public boolean isPermissionGranted(RbacScope scope, Collection<String> roleNames, String permissionName) {
        if (scope == null || roleNames == null || roleNames.isEmpty() || !StringUtils.hasText(permissionName)) {
            return false;
        }
        String normalizedPerm = normalizePermission(permissionName);
        Map<String, Set<String>> scopedRoles = roleIndex.getOrDefault(scope, Collections.emptyMap());
        for (String role : roleNames) {
            if (!StringUtils.hasText(role)) {
                continue;
            }
            Set<String> grants = scopedRoles.get(role.toUpperCase(Locale.US));
            if (grants != null && grants.contains(normalizedPerm)) {
                return true;
            }
        }
        return false;
    }

    private void parsePermissions(JsonNode permissionsNode) {
        if (permissionsNode == null || !permissionsNode.isArray()) {
            return;
        }
        for (JsonNode node : permissionsNode) {
            String name = node.path("name").asText(null);
            if (!StringUtils.hasText(name)) {
                continue;
            }
            RbacScope scope = RbacScope.from(node.path("scope").asText(null));
            List<String> resources = parseDelimitedList(node.path("resources"), ",");
            List<String> conditions = parseDelimitedList(node.path("conditions"), ";");
            JsonNode abacNode = node.path("abac");
            RbacPermissionDefinition.AbacRules rules = RbacPermissionDefinition.AbacRules.builder()
                    .isAuthenticated(abacNode.path("is_authenticated").asBoolean(false))
                    .ownsScope(abacNode.path("owns_scope").asBoolean(false))
                    .isMember(abacNode.path("is_member").asBoolean(false))
                    .build();

            RbacPermissionDefinition definition = RbacPermissionDefinition.builder()
                    .name(normalizePermission(name))
                    .scope(scope)
                    .resources(resources)
                    .description(node.path("description").asText(""))
                    .conditions(conditions)
                    .abac(rules)
                    .build();
            permissionIndex.put(definition.getName(), definition);
        }
    }

    private void parseRoles(JsonNode rolesNode) {
        if (rolesNode == null || !rolesNode.isObject()) {
            return;
        }
        rolesNode.fields().forEachRemaining(scopeEntry -> {
            RbacScope scope = RbacScope.from(scopeEntry.getKey());
            JsonNode scopeRoles = scopeEntry.getValue();
            if (!scopeRoles.isObject()) {
                return;
            }
            Map<String, Set<String>> scopedMap = roleIndex.computeIfAbsent(scope, s -> new HashMap<>());
            scopeRoles.fields().forEachRemaining(roleEntry -> {
                String roleName = roleEntry.getKey().toUpperCase(Locale.US);
                Set<String> permissions = scopedMap.computeIfAbsent(roleName, rn -> new HashSet<>());
                JsonNode permsArray = roleEntry.getValue();
                if (permsArray.isArray()) {
                    for (JsonNode permNode : permsArray) {
                        String permName = permNode.asText(null);
                        if (StringUtils.hasText(permName)) {
                            permissions.add(normalizePermission(permName));
                        }
                    }
                } else if (permsArray.isTextual()) {
                    permissions.add(normalizePermission(permsArray.asText()));
                }
            });
        });
    }

    private List<String> parseDelimitedList(JsonNode node, String delimiter) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(item -> {
                if (item.isTextual()) {
                    values.add(item.asText().trim());
                }
            });
            return values.stream().filter(StringUtils::hasText).toList();
        }
        String raw = node.asText(null);
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String[] tokens = raw.split(delimiter);
        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String normalizePermission(String permission) {
        return permission == null ? "" : permission.trim().toLowerCase(Locale.US);
    }
}
