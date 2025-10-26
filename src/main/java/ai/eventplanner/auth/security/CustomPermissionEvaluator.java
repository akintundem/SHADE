package ai.eventplanner.auth.security;

import ai.eventplanner.auth.service.AuthorizationService;
import ai.eventplanner.auth.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

/**
 * Custom permission evaluator for method-level security
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {
    
    private final AuthorizationService authorizationService;
    
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return false;
        }
        
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        String permissionName = permission.toString();
        
        // Extract context and resource ID from target object
        String context = extractContext(targetDomainObject);
        UUID resourceId = extractResourceId(targetDomainObject);
        
        log.debug("Evaluating permission: {} for user: {} on object: {} in context: {}", 
                 permissionName, user.getId(), targetDomainObject, context);
        
        return authorizationService.hasPermission(user, permissionName, context, resourceId);
    }
    
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return false;
        }
        
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        String permissionName = permission.toString();
        
        log.debug("Evaluating permission: {} for user: {} on target: {} of type: {}", 
                 permissionName, user.getId(), targetId, targetType);
        
        return authorizationService.hasPermission(user, permissionName, targetType, UUID.fromString(targetId.toString()));
    }
    
    /**
     * Extract context from target object
     */
    private String extractContext(Object targetDomainObject) {
        if (targetDomainObject == null) {
            return "system";
        }
        
        String className = targetDomainObject.getClass().getSimpleName().toLowerCase();
        
        // Map object types to contexts
        if (className.contains("event")) {
            return "event";
        } else if (className.contains("organization")) {
            return "organization";
        } else if (className.contains("budget")) {
            return "event"; // Budget belongs to event
        } else if (className.contains("vendor")) {
            return "event"; // Vendor belongs to event
        } else if (className.contains("user")) {
            return "system";
        }
        
        return "system";
    }
    
    /**
     * Extract resource ID from target object
     */
    private UUID extractResourceId(Object targetDomainObject) {
        if (targetDomainObject == null) {
            return null;
        }
        
        try {
            // Try to get ID field using reflection
            java.lang.reflect.Field idField = targetDomainObject.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object idValue = idField.get(targetDomainObject);
            
            if (idValue instanceof UUID) {
                return (UUID) idValue;
            } else if (idValue instanceof String) {
                return UUID.fromString((String) idValue);
            }
        } catch (Exception e) {
            log.debug("Could not extract ID from object: {}", e.getMessage());
        }
        
        return null;
    }
}
