package ai.eventplanner.auth.service;

import ai.eventplanner.auth.entity.UserAccount;
import ai.eventplanner.common.domain.enums.SystemRole;
import ai.eventplanner.common.domain.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced UserPrincipal with multi-level role support
 */
@Getter
public class UserPrincipal implements UserDetails {
    
    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String name;
    private final SystemRole systemRole;
    private final boolean emailVerified;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;
    private final LocalDateTime lastLoginAt;
    
    // Context-specific roles (loaded per request)
    private List<String> organizationRoles;
    private List<String> eventRoles;
    
    public UserPrincipal(UserAccount user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.name = user.getName();
        this.systemRole = determineSystemRole(user);
        this.emailVerified = user.isEmailVerified();
        this.accountNonExpired = true;
        this.accountNonLocked = user.getStatus() != UserStatus.SUSPENDED;
        this.credentialsNonExpired = true;
        this.enabled = user.getStatus() == UserStatus.ACTIVE;
        this.lastLoginAt = user.getLastLoginAt();
    }
    
    public UserPrincipal(UserAccount user, List<String> organizationRoles, List<String> eventRoles) {
        this(user);
        this.organizationRoles = organizationRoles;
        this.eventRoles = eventRoles;
    }
    
    private SystemRole determineSystemRole(UserAccount user) {
        // For now, determine based on user type or other criteria
        // This could be enhanced with a dedicated system role field
        if (user.getUserType() != null && user.getUserType().name().equals("ADMIN")) {
            return SystemRole.ADMIN;
        }
        return SystemRole.USER;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // System-level roles
        authorities.add(new SimpleGrantedAuthority("ROLE_" + systemRole.name()));
        
        // Organization roles
        if (organizationRoles != null) {
            organizationRoles.forEach(role -> 
                authorities.add(new SimpleGrantedAuthority("ORG_" + role)));
        }
        
        // Event-specific roles
        if (eventRoles != null) {
            eventRoles.forEach(role -> 
                authorities.add(new SimpleGrantedAuthority("EVENT_" + role)));
        }
        
        return authorities;
    }
    
    @Override
    public String getPassword() {
        return passwordHash;
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(role));
    }
    
    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(String... roles) {
        return getAuthorities().stream()
            .anyMatch(authority -> {
                String authorityRole = authority.getAuthority();
                for (String role : roles) {
                    if (authorityRole.equals(role)) {
                        return true;
                    }
                }
                return false;
            });
    }
    
    /**
     * Check if user has system admin privileges
     */
    public boolean isSystemAdmin() {
        return systemRole == SystemRole.SUPER_ADMIN || systemRole == SystemRole.ADMIN;
    }
    
    /**
     * Check if user has organization admin privileges
     */
    public boolean isOrganizationAdmin() {
        return organizationRoles != null && 
               (organizationRoles.contains("OWNER") || 
                organizationRoles.contains("MANAGER"));
    }
    
    /**
     * Check if user has event organizer privileges
     */
    public boolean isEventOrganizer() {
        return eventRoles != null && eventRoles.contains("ORGANIZER");
    }
    
    /**
     * Get the underlying user account
     */
    public UserAccount getUser() {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setName(name);
        user.setEmailVerified(emailVerified);
        user.setLastLoginAt(lastLoginAt);
        return user;
    }
}
