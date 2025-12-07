package eventplanner.common.domain.entity;

import eventplanner.common.domain.enums.ActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Unified Audit Log entity for tracking all changes across all domains
 * This provides a comprehensive, centralized audit trail for compliance and debugging
 * 
 * Replaces: SecurityAuditLog, AttendeeAuditLog, and domain-specific audit logs
 * 
 * Supports:
 * - Security events (login, logout, password changes)
 * - Entity changes (CRUD operations)
 * - Business workflows (approvals, submissions)
 * - Attendee operations (check-in, RSVP)
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_domain", columnList = "domain"),
    @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_email", columnList = "email"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_action", columnList = "action_type"),
    @Index(name = "idx_audit_event", columnList = "event_id"),
    @Index(name = "idx_audit_status", columnList = "status"),
    @Index(name = "idx_audit_device", columnList = "device_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Domain/module this audit log belongs to
     * Examples: "SECURITY", "BUDGET", "ATTENDEE", "EVENT", "VENDOR", etc.
     */
    @Column(name = "domain", nullable = false, length = 50)
    private String domain;
    
    /**
     * Type of entity being audited (e.g., "Budget", "User", "Attendee")
     * Can be null for domain-level actions (e.g., bulk operations)
     */
    @Column(name = "entity_type", length = 100)
    private String entityType;
    
    /**
     * ID of the specific entity being audited
     * Can be null for domain-level actions
     */
    @Column(name = "entity_id")
    private UUID entityId;
    
    /**
     * Action performed (CREATE, UPDATE, DELETE, LOGIN, CHECK_IN, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;
    
    /**
     * Status of the action (SUCCESS, FAILURE, BLOCKED, WARNING, PENDING)
     */
    @Column(name = "status", length = 20)
    private String status;
    
    /**
     * ID of the user who performed the action
     */
    @Column(name = "user_id")
    private UUID userId;
    
    /**
     * Username of the user who performed the action
     */
    @Column(name = "username", length = 255)
    private String username;
    
    /**
     * Email of the user who performed the action
     */
    @Column(name = "email", length = 180)
    private String email;
    
    /**
     * Human-readable description of what changed
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * JSON representation of the old values (before change)
     */
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;
    
    /**
     * JSON representation of the new values (after change)
     */
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;
    
    /**
     * IP address of the user
     */
    @Column(name = "ip_address", length = 60)
    private String ipAddress;
    
    /**
     * User agent (browser/client information)
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Device ID for tracking device-specific actions
     */
    @Column(name = "device_id", length = 120)
    private String deviceId;
    
    /**
     * Risk level for security-related audits (LOW, MEDIUM, HIGH, CRITICAL)
     */
    @Column(name = "risk_level", length = 20)
    private String riskLevel;
    
    /**
     * Session ID for tracking session-specific actions
     */
    @Column(name = "session_id")
    private UUID sessionId;
    
    /**
     * Idempotency key for tracking duplicate operations
     */
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;
    
    /**
     * Additional domain-specific metadata (JSON format)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * Event ID for event-scoped audits (optional, for easier querying)
     */
    @Column(name = "event_id")
    private UUID eventId;
    
    /**
     * Timestamp of the action
     */
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (status == null) {
            status = "SUCCESS";
        }
    }
}

