package ai.eventplanner.auth.service;

import ai.eventplanner.auth.entity.Permission;
import ai.eventplanner.auth.entity.RolePermission;
import ai.eventplanner.auth.repo.PermissionRepository;
import ai.eventplanner.auth.repo.RolePermissionRepository;
import ai.eventplanner.common.domain.enums.ActionType;
import ai.eventplanner.common.domain.enums.ResourceType;
import ai.eventplanner.common.domain.enums.RoleName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service to initialize RBAC permissions and role mappings using enums
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RbacInitializationService implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializePermissions();
        initializeRolePermissions();
        log.info("RBAC initialization completed");
    }

    private void initializePermissions() {
        if (permissionRepository.count() > 0) {
            log.info("Permissions already initialized, skipping...");
            return;
        }

        List<Permission> permissions = List.of(
            // Event permissions
            createPermission(ResourceType.EVENT, ActionType.CREATE, "Create events", true, false, true),
            createPermission(ResourceType.EVENT, ActionType.READ, "Read event details", true, false, true),
            createPermission(ResourceType.EVENT, ActionType.UPDATE, "Update event details", true, false, true),
            createPermission(ResourceType.EVENT, ActionType.DELETE, "Delete events", true, false, true),
            createPermission(ResourceType.EVENT, ActionType.MANAGE, "Manage all event aspects", true, false, true),

            // Budget permissions
            createPermission(ResourceType.BUDGET, ActionType.CREATE, "Create budgets", true, false, true),
            createPermission(ResourceType.BUDGET, ActionType.READ, "Read budget details", true, false, true),
            createPermission(ResourceType.BUDGET, ActionType.UPDATE, "Update budget details", true, false, true),
            createPermission(ResourceType.BUDGET, ActionType.DELETE, "Delete budgets", true, false, true),
            createPermission(ResourceType.BUDGET, ActionType.MANAGE, "Manage all budget aspects", true, false, true),

            // Vendor permissions
            createPermission(ResourceType.VENDOR, ActionType.CREATE, "Create vendors", true, false, true),
            createPermission(ResourceType.VENDOR, ActionType.READ, "Read vendor details", true, false, true),
            createPermission(ResourceType.VENDOR, ActionType.UPDATE, "Update vendor details", true, false, true),
            createPermission(ResourceType.VENDOR, ActionType.DELETE, "Delete vendors", true, false, true),
            createPermission(ResourceType.VENDOR, ActionType.MANAGE, "Manage all vendor aspects", true, false, true),

            // Role permissions
            createPermission(ResourceType.ROLE, ActionType.ASSIGN, "Assign roles to users", true, false, true),
            createPermission(ResourceType.ROLE, ActionType.REMOVE, "Remove roles from users", true, false, true),
            createPermission(ResourceType.ROLE, ActionType.READ, "Read role assignments", true, false, true),
            createPermission(ResourceType.ROLE, ActionType.MANAGE, "Manage all role aspects", true, false, true),

            // Organization permissions
            createPermission(ResourceType.ORGANIZATION, ActionType.CREATE, "Create organizations", true, true, false),
            createPermission(ResourceType.ORGANIZATION, ActionType.READ, "Read organization details", true, true, false),
            createPermission(ResourceType.ORGANIZATION, ActionType.UPDATE, "Update organization details", true, true, false),
            createPermission(ResourceType.ORGANIZATION, ActionType.DELETE, "Delete organizations", true, true, false),
            createPermission(ResourceType.ORGANIZATION, ActionType.MANAGE, "Manage all organization aspects", true, true, false),

            // User permissions
            createPermission(ResourceType.USER, ActionType.CREATE, "Create users", true, false, false),
            createPermission(ResourceType.USER, ActionType.READ, "Read user details", true, false, false),
            createPermission(ResourceType.USER, ActionType.UPDATE, "Update user details", true, false, false),
            createPermission(ResourceType.USER, ActionType.DELETE, "Delete users", true, false, false),
            createPermission(ResourceType.USER, ActionType.MANAGE, "Manage all user aspects", true, false, false),

            // Weather permissions
            createPermission(ResourceType.WEATHER, ActionType.READ, "Read weather data", true, false, true)
        );

        permissionRepository.saveAll(permissions);
        log.info("Initialized {} permissions", permissions.size());
    }

    private Permission createPermission(ResourceType resource, ActionType action, String description, 
                                      boolean isSystemPermission, boolean isOrganizationPermission, boolean isEventPermission) {
        String name = Permission.generatePermissionName(resource, action);
        return Permission.builder()
            .name(name)
            .description(description)
            .resource(resource)
            .action(action)
            .isSystemPermission(isSystemPermission)
            .isOrganizationPermission(isOrganizationPermission)
            .isEventPermission(isEventPermission)
            .build();
    }

    private void initializeRolePermissions() {
        if (rolePermissionRepository.count() > 0) {
            log.info("Role permissions already initialized, skipping...");
            return;
        }

        // Event Organizer permissions
        createRolePermissions(RoleName.ORGANIZER, List.of(
            "event.create", "event.read", "event.update", "event.delete", "event.manage",
            "budget.create", "budget.read", "budget.update", "budget.delete", "budget.manage",
            "vendor.create", "vendor.read", "vendor.update", "vendor.delete", "vendor.manage",
            "role.assign", "role.remove", "role.read", "role.manage",
            "weather.read"
        ), "event");

        // Event Coordinator permissions
        createRolePermissions(RoleName.COORDINATOR, List.of(
            "event.read", "event.update",
            "budget.read", "budget.update",
            "vendor.read", "vendor.update",
            "role.assign", "role.read",
            "weather.read"
        ), "event");

        // Event Staff permissions
        createRolePermissions(RoleName.STAFF, List.of(
            "event.read",
            "budget.read",
            "vendor.read",
            "weather.read"
        ), "event");

        // Event Volunteer permissions
        createRolePermissions(RoleName.VOLUNTEER, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Security permissions
        createRolePermissions(RoleName.SECURITY, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Technical permissions
        createRolePermissions(RoleName.TECHNICAL, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Catering permissions
        createRolePermissions(RoleName.CATERING, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Cleanup permissions
        createRolePermissions(RoleName.CLEANUP, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Registration permissions
        createRolePermissions(RoleName.REGISTRATION, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Photographer permissions
        createRolePermissions(RoleName.PHOTOGRAPHER, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Videographer permissions
        createRolePermissions(RoleName.VIDEographer, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // DJ permissions
        createRolePermissions(RoleName.DJ, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // MC permissions
        createRolePermissions(RoleName.MC, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Speaker permissions
        createRolePermissions(RoleName.SPEAKER, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Moderator permissions
        createRolePermissions(RoleName.MODERATOR, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Sponsor permissions
        createRolePermissions(RoleName.SPONSOR, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Vendor permissions
        createRolePermissions(RoleName.VENDOR, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Media permissions
        createRolePermissions(RoleName.MEDIA, List.of(
            "event.read",
            "weather.read"
        ), "event");

        // Guest permissions
        createRolePermissions(RoleName.GUEST, List.of(
            "event.read",
            "weather.read"
        ), "event");

        log.info("Initialized role permissions");
    }

    private void createRolePermissions(RoleName roleName, List<String> permissionNames, String context) {
        List<Permission> permissions = permissionRepository.findByNameIn(permissionNames);
        
        List<RolePermission> rolePermissions = permissions.stream()
            .map(permission -> RolePermission.builder()
                .roleName(roleName)
                .permission(permission)
                .context(context)
                .isGranted(true)
                .build())
            .toList();

        rolePermissionRepository.saveAll(rolePermissions);
    }
}
