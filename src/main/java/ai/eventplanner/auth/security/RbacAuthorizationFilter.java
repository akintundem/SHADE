package ai.eventplanner.auth.security;

import ai.eventplanner.auth.service.AuthorizationService;
import ai.eventplanner.auth.service.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RBAC Filter for authorization at the filter level
 * Handles permission checking based on URL patterns and HTTP methods
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RbacAuthorizationFilter extends OncePerRequestFilter {

    private final AuthorizationService authorizationService;

    // URL patterns and their required permissions
    private static final Map<Pattern, Map<String, String>> URL_PERMISSIONS = createUrlPermissions();
    
    private static Map<Pattern, Map<String, String>> createUrlPermissions() {
        Map<Pattern, Map<String, String>> permissions = new java.util.HashMap<>();
        
        // Event management patterns
        permissions.put(Pattern.compile("^/api/v1/events$"), Map.of(
            "POST", "event.create",
            "GET", "event.read"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/events/([a-f0-9-]+)$"), Map.of(
            "GET", "event.read",
            "PUT", "event.update",
            "DELETE", "event.delete"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/events/([a-f0-9-]+)/.*$"), Map.of(
            "GET", "event.read",
            "POST", "event.update",
            "PUT", "event.update",
            "DELETE", "event.delete"
        ));

        // Budget management patterns (corrected URLs)
        permissions.put(Pattern.compile("^/api/v1/budgets$"), Map.of(
            "POST", "budget.create",
            "GET", "budget.read"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/budgets/([a-f0-9-]+)$"), Map.of(
            "GET", "budget.read",
            "PUT", "budget.update",
            "DELETE", "budget.delete"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/budgets/([a-f0-9-]+)/.*$"), Map.of(
            "GET", "budget.read",
            "POST", "budget.update",
            "PUT", "budget.update",
            "DELETE", "budget.delete"
        ));

        // Vendor management patterns
        permissions.put(Pattern.compile("^/api/v1/vendors$"), Map.of(
            "POST", "vendor.create",
            "GET", "vendor.read"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/vendors/([a-f0-9-]+)$"), Map.of(
            "GET", "vendor.read",
            "PUT", "vendor.update",
            "DELETE", "vendor.delete"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/events/([a-f0-9-]+)/vendors.*$"), Map.of(
            "GET", "vendor.read",
            "POST", "vendor.create",
            "PUT", "vendor.update",
            "DELETE", "vendor.delete"
        ));

        // Role management patterns
        permissions.put(Pattern.compile("^/api/v1/roles/events/([a-f0-9-]+)/assign$"), Map.of(
            "POST", "role.assign"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/roles/events/([a-f0-9-]+)/users/([a-f0-9-]+)/roles/([^/]+)$"), Map.of(
            "DELETE", "role.remove"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/roles/events/([a-f0-9-]+)/.*$"), Map.of(
            "GET", "role.read"
        ));

        // Organization management patterns (corrected URLs)
        permissions.put(Pattern.compile("^/api/v1/auth/organizations$"), Map.of(
            "POST", "organization.create",
            "GET", "organization.read"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/auth/organizations/([a-f0-9-]+)$"), Map.of(
            "GET", "organization.read",
            "PUT", "organization.update",
            "DELETE", "organization.delete"
        ));

        // User management patterns (corrected URLs)
        permissions.put(Pattern.compile("^/api/v1/auth/users$"), Map.of(
            "POST", "user.create",
            "GET", "user.read"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/auth/users/([a-f0-9-]+)$"), Map.of(
            "GET", "user.read",
            "PUT", "user.update",
            "DELETE", "user.delete"
        ));

        // Timeline management patterns
        permissions.put(Pattern.compile("^/api/v1/timeline/([a-f0-9-]+)$"), Map.of(
            "GET", "timeline.read",
            "POST", "timeline.create",
            "PUT", "timeline.update",
            "DELETE", "timeline.delete"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/timeline/([a-f0-9-]+)/.*$"), Map.of(
            "GET", "timeline.read",
            "POST", "timeline.create",
            "PUT", "timeline.update",
            "DELETE", "timeline.delete"
        ));

        // Risk management patterns
        permissions.put(Pattern.compile("^/api/v1/risks/([a-f0-9-]+)$"), Map.of(
            "GET", "risk.read"
        ));

        // Weather patterns (public endpoints)
        permissions.put(Pattern.compile("^/api/v1/weather/.*$"), Map.of(
            "GET", "weather.read"
        ));

        // Attendee management patterns
        permissions.put(Pattern.compile("^/api/v1/events/([a-f0-9-]+)/attendances.*$"), Map.of(
            "GET", "attendee.read",
            "POST", "attendee.create",
            "PUT", "attendee.update",
            "DELETE", "attendee.delete"
        ));
        
        permissions.put(Pattern.compile("^/api/v1/events/([a-f0-9-]+)/users.*$"), Map.of(
            "GET", "attendee.read",
            "POST", "attendee.create",
            "PUT", "attendee.update",
            "DELETE", "attendee.delete"
        ));

        // Platform payment patterns
        permissions.put(Pattern.compile("^/api/platform-payments.*$"), Map.of(
            "GET", "payment.read",
            "POST", "payment.create",
            "PUT", "payment.update"
        ));

        // Admin patterns
        permissions.put(Pattern.compile("^/api/v1/admin/.*$"), Map.of(
            "GET", "admin.read",
            "POST", "admin.create",
            "PUT", "admin.update",
            "DELETE", "admin.delete"
        ));
        
        return permissions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("RBAC Filter checking: {} {}", method, requestURI);
        
        // Skip authorization for public endpoints
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Get authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            log.warn("No valid authentication found for request: {} {}", method, requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        
        // Check permissions
        if (!hasPermission(user, requestURI, method)) {
            log.warn("Access denied for user {} to {} {}", user.getId(), method, requestURI);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"Insufficient permissions\"}");
            response.setContentType("application/json");
            return;
        }
        
        log.debug("Access granted for user {} to {} {}", user.getId(), method, requestURI);
        filterChain.doFilter(request, response);
    }
    
    /**
     * Check if the endpoint is public (no authorization required)
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/v1/auth/login") ||
               requestURI.startsWith("/api/v1/auth/register") ||
               requestURI.startsWith("/api/v1/auth/refresh") ||
               requestURI.startsWith("/api/v1/auth/verify-email") ||
               requestURI.startsWith("/api/v1/auth/reset-password") ||
               requestURI.startsWith("/api/v1/auth/forgot-password") ||
               requestURI.startsWith("/api/v1/auth/validate-token") ||
               requestURI.startsWith("/health") ||
               requestURI.startsWith("/actuator/health") ||
               requestURI.startsWith("/actuator/info") ||
               requestURI.startsWith("/api/v1/weather") ||
               requestURI.equals("/") ||
               requestURI.startsWith("/error") ||
               requestURI.startsWith("/favicon.ico") ||
               requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/");
    }
    
    /**
     * Check if user has permission for the request
     */
    private boolean hasPermission(UserPrincipal user, String requestURI, String method) {
        // Find matching URL pattern
        for (Map.Entry<Pattern, Map<String, String>> entry : URL_PERMISSIONS.entrySet()) {
            Pattern pattern = entry.getKey();
            Matcher matcher = pattern.matcher(requestURI);
            
            if (matcher.matches()) {
                Map<String, String> methodPermissions = entry.getValue();
                String requiredPermission = methodPermissions.get(method);
                
                if (requiredPermission != null) {
                    // Extract resource ID from URL if present
                    UUID resourceId = extractResourceId(matcher, requestURI);
                    String context = determineContext(requestURI);
                    
                    log.debug("Checking permission: {} for user: {} in context: {} for resource: {}", 
                             requiredPermission, user.getId(), context, resourceId);
                    
                    // Handle special authorization cases
                    if (handleSpecialAuthorizationCases(user, requestURI, method, resourceId)) {
                        return true;
                    }
                    
                    return authorizationService.hasPermission(user, requiredPermission, context, resourceId);
                }
            }
        }
        
        // If no specific pattern matches, check for general authenticated access
        log.debug("No specific permission pattern found for: {} {}, allowing authenticated access", method, requestURI);
        return true;
    }
    
    /**
     * Handle special authorization cases that require custom logic
     */
    private boolean handleSpecialAuthorizationCases(UserPrincipal user, String requestURI, String method, UUID resourceId) {
        // User can only access their own profile
        if (requestURI.matches("^/api/v1/auth/users/([a-f0-9-]+)$") && 
            (method.equals("GET") || method.equals("PUT"))) {
            return user.getId().equals(resourceId);
        }
        
        // User search is restricted to administrators only
        if (requestURI.equals("/api/v1/auth/users/search") && method.equals("GET")) {
            return user.isSystemAdmin();
        }
        
        // Organization ownership check
        if (requestURI.matches("^/api/v1/organizations/([a-f0-9-]+)$") && 
            (method.equals("PUT") || method.equals("DELETE"))) {
            return isOrganizationOwner(user, resourceId);
        }
        
        // Event ownership check for sensitive operations
        if (requestURI.matches("^/api/v1/events/([a-f0-9-]+)$") && method.equals("DELETE")) {
            return isEventOwner(user, resourceId);
        }
        
        // Role assignment permissions
        if (requestURI.matches("^/api/v1/roles/events/([a-f0-9-]+)/assign$") && method.equals("POST")) {
            return canAssignEventRoles(user, resourceId);
        }
        
        if (requestURI.matches("^/api/v1/roles/events/([a-f0-9-]+)/users/([a-f0-9-]+)/roles/([^/]+)$") && method.equals("DELETE")) {
            return canRemoveEventRoles(user, resourceId);
        }
        
        return false; // Let normal permission checking handle it
    }
    
    /**
     * Check if user is organization owner
     */
    private boolean isOrganizationOwner(UserPrincipal user, UUID organizationId) {
        // This would need to be implemented with proper organization role checking
        // For now, return true for system admins
        return user.isSystemAdmin();
    }
    
    /**
     * Check if user is event owner
     */
    private boolean isEventOwner(UserPrincipal user, UUID eventId) {
        // This would need to be implemented with proper event ownership checking
        // For now, return true for system admins
        return user.isSystemAdmin();
    }
    
    /**
     * Check if user can assign event roles
     */
    private boolean canAssignEventRoles(UserPrincipal user, UUID eventId) {
        // System admins can assign any role
        if (user.isSystemAdmin()) {
            return true;
        }
        
        // Event organizers can assign roles
        if (user.isEventOrganizer()) {
            return true;
        }
        
        // This would need to be enhanced with proper event role checking
        return false;
    }
    
    /**
     * Check if user can remove event roles
     */
    private boolean canRemoveEventRoles(UserPrincipal user, UUID eventId) {
        // System admins can remove any role
        if (user.isSystemAdmin()) {
            return true;
        }
        
        // Event organizers can remove roles
        if (user.isEventOrganizer()) {
            return true;
        }
        
        // This would need to be enhanced with proper event role checking
        return false;
    }
    
    /**
     * Extract resource ID from URL matcher
     */
    private UUID extractResourceId(Matcher matcher, String requestURI) {
        try {
            // Try to extract UUID from the first group
            if (matcher.groupCount() > 0) {
                String idString = matcher.group(1);
                return UUID.fromString(idString);
            }
        } catch (Exception e) {
            log.debug("Could not extract resource ID from URL: {}", requestURI);
        }
        return null;
    }
    
    /**
     * Determine context from request URI
     */
    private String determineContext(String requestURI) {
        if (requestURI.contains("/events/")) {
            return "event";
        } else if (requestURI.contains("/organizations/")) {
            return "organization";
        } else if (requestURI.contains("/admin/")) {
            return "system";
        } else if (requestURI.contains("/users/")) {
            return "system";
        }
        return "system";
    }
}
