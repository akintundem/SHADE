package eventplanner.common.communication.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "device_token", nullable = false, unique = true, length = 1000)
    private String deviceToken;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private Platform platform;
    
    @Column(name = "device_id")
    private String deviceId;
    
    @Column(name = "app_version")
    private String appVersion;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "invalidated_at")
    private LocalDateTime invalidatedAt;
    
    @Column(name = "failure_count", nullable = false)
    @Builder.Default
    private Integer failureCount = 0;
    
    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;
    
    @Column(name = "last_failure_reason", length = 500)
    private String lastFailureReason;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum Platform {
        IOS, ANDROID
    }
    
    public DeviceToken(UUID userId, String deviceToken, Platform platform) {
        this.userId = userId;
        this.deviceToken = deviceToken;
        this.platform = platform;
        this.isActive = true;
    }
    
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
    
    public void deactivate() {
        this.isActive = false;
        this.invalidatedAt = LocalDateTime.now();
    }
    
    /**
     * Mark token as invalid due to push service error
     */
    public void markAsInvalid(String reason) {
        this.isActive = false;
        this.invalidatedAt = LocalDateTime.now();
        this.failureCount = (this.failureCount != null ? this.failureCount : 0) + 1;
        this.lastFailureAt = LocalDateTime.now();
        this.lastFailureReason = reason;
    }
    
    /**
     * Increment failure count (for transient failures)
     */
    public void recordFailure(String reason) {
        this.failureCount = (this.failureCount != null ? this.failureCount : 0) + 1;
        this.lastFailureAt = LocalDateTime.now();
        this.lastFailureReason = reason;
    }
    
    /**
     * Reset failure count after successful send
     */
    public void resetFailureCount() {
        this.failureCount = 0;
        this.lastFailureAt = null;
        this.lastFailureReason = null;
    }
    
    /**
     * Check if token should be considered invalid based on failure count
     */
    public boolean shouldBeInvalidated(int maxFailures) {
        return (this.failureCount != null && this.failureCount >= maxFailures);
    }
}
