package ai.eventplanner.auth.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Client Application entity for tracking and validating client applications
 * Each client (web app, mobile app, API client) must be registered
 */
@Entity
@Table(
    name = "client_applications",
    indexes = {
        @Index(name = "idx_client_applications_client_id", columnList = "clientId"),
        @Index(name = "idx_client_applications_active", columnList = "active")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class ClientApplication extends BaseEntity {

    @Column(name = "client_id", nullable = false, unique = true, length = 120)
    private String clientId;

    @Column(name = "client_name", nullable = false, length = 255)
    private String clientName;

    @Column(name = "client_secret_hash", nullable = false, length = 255)
    private String clientSecretHash;

    @Column(name = "client_type", nullable = false, length = 50)
    private String clientType; // WEB, MOBILE, API, DESKTOP

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "allowed_origins", columnDefinition = "TEXT")
    private String allowedOrigins; // Comma-separated list of allowed origins

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @Column(name = "rate_limit_per_minute")
    private Integer rateLimitPerMinute = 100;

    @Column(name = "rate_limit_per_hour")
    private Integer rateLimitPerHour = 1000;

    @Column(name = "max_concurrent_sessions")
    private Integer maxConcurrentSessions = 5;

    @PrePersist
    public void onCreate() {
        if (lastUsed == null) {
            lastUsed = LocalDateTime.now();
        }
    }

    public void updateLastUsed() {
        this.lastUsed = LocalDateTime.now();
    }
}
