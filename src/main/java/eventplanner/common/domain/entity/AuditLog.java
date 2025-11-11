package eventplanner.common.domain.entity;

import eventplanner.common.domain.enums.ActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log entity for tracking changes to entities
 * This provides a comprehensive audit trail for compliance and debugging
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_action", columnList = "action_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Type of entity being audited (e.g., "Budget", "BudgetLineItem")
     */
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;
    
    /**
     * ID of the entity being audited
     */
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    
    /**
     * Action performed (CREATE, UPDATE, DELETE, APPROVE, REJECT, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;
    
    /**
     * ID of the user who performed the action
     */
    @Column(name = "user_id")
    private UUID userId;
    
    /**
     * Username or email of the user who performed the action
     */
    @Column(name = "username", length = 255)
    private String username;
    
    /**
     * Description of what changed
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
     * IP address of the user (if available)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User agent (browser/client information)
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Additional metadata (JSON format)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * Timestamp of the action
     */
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    /**
     * Event ID for budget-related audits (optional, for easier querying)
     */
    @Column(name = "event_id")
    private UUID eventId;
}

