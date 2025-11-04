package eventplanner.common.communication.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
@Data
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
    private Boolean isActive = true;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
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
    }
}
