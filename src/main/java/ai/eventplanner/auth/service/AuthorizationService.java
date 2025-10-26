package ai.eventplanner.auth.service;

import ai.eventplanner.auth.entity.OrganizationRole;
import ai.eventplanner.auth.entity.RolePermission;
import ai.eventplanner.auth.repo.OrganizationRoleRepository;
import ai.eventplanner.auth.repo.RolePermissionRepository;
import ai.eventplanner.roles.entity.EventRole;
import ai.eventplanner.roles.repo.EventRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling role-based access control (RBAC)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {
    
    private final RolePermissionRepository rolePermissionRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final EventRoleRepository eventRoleRepository;
    
    /**
     * Check if user has permission in any context
     */
    public boolean hasPermission(UserPrincipal user, String permissionName, String context, UUID resourceId) {
        if (user == null || permissionName == null) {
            return false;
        }
        
        log.debug("Checking permission: {} for user: {} in context: {} for resource: {}", 
                 permissionName, user.getId(), context, resourceId);
        
        // Check system-level permissions
        if (hasSystemPermission(user, permissionName)) {
            log.debug("User {} has system permission: {}", user.getId(), permissionName);
            return true;
        }
        
        // Check organization-level permissions
        if (hasOrganizationPermission(user, permissionName, context, resourceId)) {
            log.debug("User {} has organization permission: {} for resource: {}", 
                     user.getId(), permissionName, resourceId);
            return true;
        }
        
        // Check event-level permissions
        if (hasEventPermission(user, permissionName, context, resourceId)) {
            log.debug("User {} has event permission: {} for resource: {}", 
                     user.getId(), permissionName, resourceId);
            return true;
        }
        
        log.debug("User {} does not have permission: {}", user.getId(), permissionName);
        return false;
    }
    
    /**
     * Check system-level permissions
     */
    private boolean hasSystemPermission(UserPrincipal user, String permissionName) {
        // Super admins have all permissions
        if (user.hasRole("ROLE_SUPER_ADMIN")) {
            return true;
        }
        
        // Check if user has system role that grants this permission
        return user.getAuthorities().stream()
            .anyMatch(authority -> {
                String roleName = authority.getAuthority().replace("ROLE_", "");
                return hasRolePermission(roleName, permissionName, "system");
            });
    }
    
    /**
     * Check organization-level permissions
     */
    private boolean hasOrganizationPermission(UserPrincipal user, String permissionName, String context, UUID resourceId) {
        if (resourceId == null) {
            return false;
        }
        
        // Get user's organization roles
        List<OrganizationRole> orgRoles = organizationRoleRepository.findByUserIdAndActive(user.getId());
        
        return orgRoles.stream()
            .anyMatch(orgRole -> {
                // Check if user has permission through organization role
                return hasRolePermission(orgRole.getRole(), permissionName, "organization") &&
                       orgRole.getOrganizationId().equals(resourceId);
            });
    }
    
    /**
     * Check event-level permissions
     */
    private boolean hasEventPermission(UserPrincipal user, String permissionName, String context, UUID resourceId) {
        if (resourceId == null) {
            return false;
        }
        
        // Load user's roles for specific event
        List<EventRole> eventRoles = eventRoleRepository.findByEventIdAndUserId(resourceId, user.getId());
        
        return eventRoles.stream()
            .filter(EventRole::getIsActive)
            .anyMatch(eventRole -> hasRolePermission(eventRole.getRoleName().name(), permissionName, "event"));
    }
    
    /**
     * Check if a role has a specific permission in a context
     */
    private boolean hasRolePermission(String roleName, String permissionName, String context) {
        try {
            // Try to find role permission mapping
            Optional<RolePermission> rolePermission = rolePermissionRepository.findByRoleNameAndPermissionNameAndContext(
                ai.eventplanner.common.domain.enums.RoleName.valueOf(roleName), 
                permissionName, 
                context
            );
            
            return rolePermission.map(RolePermission::getIsGranted).orElse(false);
                
        } catch (IllegalArgumentException e) {
            // Role name not found in enum, check string-based roles
            log.debug("Role {} not found in enum, checking string-based permissions", roleName);
            return false;
        }
    }
    
    /**
     * Check if user can assign a specific role
     */
    public boolean canAssignRole(UserPrincipal user, String roleName, String context, UUID resourceId) {
        String assignPermission = "role.assign." + roleName.toLowerCase();
        return hasPermission(user, assignPermission, context, resourceId);
    }
    
    /**
     * Get all permissions for a user in a specific context
     */
    public List<String> getUserPermissions(UserPrincipal user, String context, UUID resourceId) {
        // This would be implemented to return all permissions the user has
        // in the given context and resource
        return List.of(); // Placeholder implementation
    }
    
    /**
     * Check if user owns a resource
     */
    public boolean isResourceOwner(UserPrincipal user, String resourceType, UUID resourceId) {
        // Implementation would depend on resource type
        // For events, check if user is the owner
        // For organizations, check if user has OWNER role
        return false; // Placeholder implementation
    }
}
