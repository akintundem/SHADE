package eventplanner.security.authorization.service;

import eventplanner.security.authorization.domain.entity.OrganizationRole;
import eventplanner.security.authorization.domain.repository.OrganizationRoleRepository;
import eventplanner.security.authorization.domain.entity.EventRole;
import eventplanner.security.authorization.domain.repository.EventRoleRepository;
import eventplanner.common.domain.enums.RoleName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced service for managing event roles with RBAC
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EventRoleService {
    
    private final EventRoleRepository eventRoleRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    
    /**
     * Assign role to user for a specific event
     */
    public EventRole assignRoleToUser(UUID eventId, UUID userId, RoleName roleName, UUID assignedBy) {
        // Check if user already has this role
        if (eventRoleRepository.findByEventIdAndUserIdAndRoleName(eventId, userId, roleName).isPresent()) {
            throw new IllegalArgumentException("User already has role: " + roleName + " for this event");
        }
        
        EventRole eventRole = new EventRole();
        eventRole.setEventId(eventId);
        eventRole.setUserId(userId);
        eventRole.setRoleName(roleName);
        eventRole.setAssignedBy(assignedBy);
        eventRole.setAssignedAt(LocalDateTime.now());
        eventRole.setIsActive(true);
            
        EventRole savedRole = eventRoleRepository.save(eventRole);
        log.info("Assigned role {} to user {} for event {} by {}", roleName, userId, eventId, assignedBy);
        
        return savedRole;
    }
    
    /**
     * Remove role from user for a specific event
     */
    public void removeRoleFromUser(UUID eventId, UUID userId, RoleName roleName, UUID removedBy) {
        eventRoleRepository.findByEventIdAndUserIdAndRoleName(eventId, userId, roleName)
            .ifPresent(role -> {
                role.setIsActive(false);
                eventRoleRepository.save(role);
                log.info("Removed role {} from user {} for event {} by {}", roleName, userId, eventId, removedBy);
            });
    }
    
    /**
     * Get all roles for a user in a specific event
     */
    public List<EventRole> getUserEventRoles(UUID userId, UUID eventId) {
        return eventRoleRepository.findByEventIdAndUserId(eventId, userId);
    }
    
    /**
     * Get all users with a specific role in an event
     */
    public List<EventRole> getUsersWithRole(UUID eventId, RoleName roleName) {
        return eventRoleRepository.findByEventIdAndRoleName(eventId, roleName);
    }
    
    /**
     * Check if user has a specific role in an event
     */
    public boolean hasRole(UUID userId, UUID eventId, RoleName roleName) {
        return eventRoleRepository.findByEventIdAndUserIdAndRoleName(eventId, userId, roleName)
            .map(EventRole::getIsActive)
            .orElse(false);
    }
    
    /**
     * Check if user has any of the specified roles in an event
     */
    public boolean hasAnyRole(UUID userId, UUID eventId, List<RoleName> roleNames) {
        return eventRoleRepository.findByEventIdAndUserId(eventId, userId).stream()
            .filter(EventRole::getIsActive)
            .anyMatch(role -> roleNames.contains(role.getRoleName()));
    }
    
    /**
     * Get role hierarchy for an event
     */
    public List<EventRole> getEventRoleHierarchy(UUID eventId) {
        return eventRoleRepository.findByEventIdAndIsActive(eventId, true);
    }
    
    /**
     * Assign organization role to user
     */
    public OrganizationRole assignOrganizationRole(UUID userId, UUID organizationId, String role, UUID assignedBy) {
        OrganizationRole organizationRole = new OrganizationRole();
        organizationRole.setUserId(userId);
        organizationRole.setOrganizationId(organizationId);
        organizationRole.setRole(role);
        organizationRole.setAssignedBy(assignedBy);
        organizationRole.setAssignedAt(LocalDateTime.now());
        organizationRole.setIsActive(true);
            
        OrganizationRole savedRole = organizationRoleRepository.save(organizationRole);
        log.info("Assigned organization role {} to user {} for organization {} by {}", 
                role, userId, organizationId, assignedBy);
        
        return savedRole;
    }
    
    /**
     * Get user's organization roles
     */
    public List<OrganizationRole> getUserOrganizationRoles(UUID userId) {
        return organizationRoleRepository.findByUserIdAndActive(userId);
    }
    
    /**
     * Check if user has organization role
     */
    public boolean hasOrganizationRole(UUID userId, UUID organizationId, String role) {
        return organizationRoleRepository.findByUserIdAndOrganizationIdAndActive(userId, organizationId).stream()
            .anyMatch(orgRole -> role.equals(orgRole.getRole()) && orgRole.getIsActive());
    }
}
