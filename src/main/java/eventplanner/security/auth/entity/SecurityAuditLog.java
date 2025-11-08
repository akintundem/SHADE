package eventplanner.security.auth.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Security audit log for tracking all security-related events.
 * Provides comprehensive audit trail for compliance and security monitoring.
 */
@Entity
@Table(
    name = "security_audit_logs",
    indexes = {
        @Index(name = "idx_security_audit_user_id", columnList = "user_id"),
        @Index(name = "idx_security_audit_event_type", columnList = "event_type"),
        @Index(name = "idx_security_audit_ip_address", columnList = "ip_address"),
        @Index(name = "idx_security_audit_created_at", columnList = "created_at"),
        @Index(name = "idx_security_audit_email", columnList = "email")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class SecurityAuditLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SecurityEventType eventType;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "client_id", length = 120)
    private String clientId;

    @Column(name = "device_id", length = 120)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SecurityEventStatus status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional context

    @Column(name = "risk_level", length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    public enum SecurityEventType {
        LOGIN_ATTEMPT,
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        REGISTRATION,
        REGISTRATION_SUCCESS,
        PASSWORD_CHANGE,
        PASSWORD_RESET_REQUEST,
        PASSWORD_RESET_SUCCESS,
        EMAIL_VERIFICATION,
        EMAIL_VERIFICATION_SUCCESS,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        RATE_LIMIT_EXCEEDED,
        TOKEN_REFRESH,
        TOKEN_VALIDATION,
        SUSPICIOUS_ACTIVITY,
        PERMISSION_DENIED,
        SESSION_CREATED,
        SESSION_REVOKED
    }

    public enum SecurityEventStatus {
        SUCCESS,
        FAILURE,
        BLOCKED,
        WARNING
    }
}

