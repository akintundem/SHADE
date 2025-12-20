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
import java.util.Objects;

/**
 * Enhanced UserPrincipal with multi-level role support
 */
@Getter
public class UserPrincipal implements UserDetails {
    
    private final UserAccount user;
    // Context-specific roles (loaded per request)
    private final List<String> eventRoles;
    private final String deviceId;
    
    public UserPrincipal(UserAccount user) {
        this(user, List.of(), null);
    }
    
    public UserPrincipal(UserAccount user, List<String> eventRoles) {
        this(user, eventRoles, null);
    }
    
    public UserPrincipal(UserAccount user, List<String> eventRoles, String deviceId) {
        this.user = user;
        this.eventRoles = eventRoles != null ? List.copyOf(eventRoles) : List.of();
        this.deviceId = deviceId;
    }
    
    public UserPrincipal withDeviceId(String deviceId) {
        if (Objects.equals(this.deviceId, deviceId)) {
            return this;
        }
        return new UserPrincipal(this.user, this.eventRoles, deviceId);
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

    public String getProfilePictureUrl() {
        return user.getProfilePictureUrl();
    }

    public java.time.LocalDateTime getLastLoginAt() {
        return user.getLastLoginAt();
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
     * Get the underlying user account
     */
    public UserAccount getUser() {
        return user;
    }
}
