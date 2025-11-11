package eventplanner.features.attendee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log for attendee operations
 * Tracks who did what, when, and from which device
 */
@Entity
@Table(
    name = "attendee_audit_logs",
    indexes = {
        @Index(name = "idx_attendee_audit_attendee_id", columnList = "attendee_id"),
        @Index(name = "idx_attendee_audit_event_id", columnList = "event_id"),
        @Index(name = "idx_attendee_audit_performed_by", columnList = "performed_by"),
        @Index(name = "idx_attendee_audit_action_type", columnList = "action_type"),
        @Index(name = "idx_attendee_audit_timestamp", columnList = "timestamp")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendeeAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @Column(name = "attendee_id")
    private UUID attendeeId;
    
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;
    
    @Column(name = "performed_by")
    private UUID performedBy;
    
    @Column(name = "performed_by_email", length = 180)
    private String performedByEmail;
    
    @Column(name = "ip_address", length = 60)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "device_id", length = 120)
    private String deviceId;
    
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional context
    
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    public enum ActionType {
        ATTENDEE_CREATED,
        ATTENDEE_UPDATED,
        ATTENDEE_DELETED,
        RSVP_UPDATED,
        CHECK_IN,
        CHECK_OUT,
        INVITATION_QUEUED,
        INVITATION_SENT,
        INVITATION_FAILED,
        BULK_IMPORT,
        BULK_UPDATE,
        EXPORT
    }
}

