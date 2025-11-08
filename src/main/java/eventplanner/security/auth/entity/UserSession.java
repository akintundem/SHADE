package eventplanner.security.auth.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_user_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class UserSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "refresh_token", nullable = false, unique = true, length = 200)
    private String refreshToken;

    @Column(name = "device_id", length = 120)
    private String deviceId;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @PrePersist
    public void onCreate() {
        if (lastSeenAt == null) {
            lastSeenAt = LocalDateTime.now();
        }
    }
    
    /**
     * Check if session is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if session is valid (not revoked and not expired)
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }
    
    /**
     * Update last seen timestamp
     */
    public void updateLastSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }
    
    /**
     * Revoke the session
     */
    public void revoke() {
        this.revoked = true;
    }
    
    /**
     * Check if session is revoked
     */
    public boolean isRevoked() {
        return this.revoked;
    }
    
    /**
     * Set revoked status
     */
    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
