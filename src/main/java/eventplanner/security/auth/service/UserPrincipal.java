package eventplanner.security.auth.service;

import eventplanner.security.auth.entity.UserAccount;
import eventplanner.common.domain.enums.SystemRole;
import eventplanner.common.domain.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Enhanced UserPrincipal with multi-level role support
 */
@Getter
public class UserPrincipal implements UserDetails {
    
    private final UserAccount user;
    // Context-specific roles (loaded per request)
    private final List<String> organizationRoles;
    private final List<String> eventRoles;
    
    public UserPrincipal(UserAccount user) {
        this(user, List.of(), List.of());
    }
    
    public UserPrincipal(UserAccount user, List<String> organizationRoles, List<String> eventRoles) {
        this.user = user;
        this.organizationRoles = organizationRoles != null ? List.copyOf(organizationRoles) : List.of();
        this.eventRoles = eventRoles != null ? List.copyOf(eventRoles) : List.of();
    }
    
    public SystemRole getSystemRole() {
        if (user.getUserType() != null && user.getUserType().name().equals("ADMIN")) {
            return SystemRole.ADMIN;
        }
        return SystemRole.USER;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // System-level roles
        authorities.add(new SimpleGrantedAuthority("ROLE_" + getSystemRole().name()));
        
        // Organization roles
        organizationRoles.forEach(role ->
            authorities.add(new SimpleGrantedAuthority("ORG_" + role)));
        
        // Event-specific roles
        eventRoles.forEach(role ->
            authorities.add(new SimpleGrantedAuthority("EVENT_" + role)));
        
        return authorities;
    }
    
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }
    
    @Override
    public String getUsername() {
        return user.getEmail();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != UserStatus.SUSPENDED;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    public boolean isEmailVerified() {
        return user.isEmailVerified();
    }

    public java.util.UUID getId() {
        return user.getId();
    }

    public String getName() {
        return user.getName();
    }

    public java.time.LocalDateTime getLastLoginAt() {
        return user.getLastLoginAt();
    }

    public List<String> getOrganizationRoles() {
        return Collections.unmodifiableList(organizationRoles);
    }

    public List<String> getEventRoles() {
        return Collections.unmodifiableList(eventRoles);
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
        SystemRole role = getSystemRole();
        return role == SystemRole.SUPER_ADMIN || role == SystemRole.ADMIN;
    }
    
    /**
     * Check if user has organization admin privileges
     */
    public boolean isOrganizationAdmin() {
        return organizationRoles.contains("OWNER") || organizationRoles.contains("MANAGER");
    }
    
    /**
     * Check if user has event organizer privileges
     */
    public boolean isEventOrganizer() {
        return eventRoles.contains("ORGANIZER");
    }
    
    /**
     * Get the underlying user account
     */
    public UserAccount getUser() {
        return user;
    }
}
