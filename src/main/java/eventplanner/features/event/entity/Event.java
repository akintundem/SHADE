package eventplanner.features.event.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.domain.enums.EventStatus;
import eventplanner.common.domain.enums.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core event entity. Relationships to other bounded contexts are represented
 * via identifier fields to keep the module decoupled.
 */
@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Event extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status")
    private EventStatus eventStatus = EventStatus.PLANNING;

    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "current_attendee_count")
    private Integer currentAttendeeCount = 0;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Column(name = "requires_approval")
    private Boolean requiresApproval = false;

    @Column(name = "qr_code_enabled")
    private Boolean qrCodeEnabled = false;

    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "event_website_url")
    private String eventWebsiteUrl;

    @Column(name = "hashtag")
    private String hashtag;

    @Column(name = "theme", columnDefinition = "TEXT")
    private String theme;

    @Column(name = "objectives", columnDefinition = "TEXT")
    private String objectives;

    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience;

    @Column(name = "success_metrics", columnDefinition = "TEXT")
    private String successMetrics;

    @Column(name = "branding_guidelines", columnDefinition = "TEXT")
    private String brandingGuidelines;

    @Column(name = "venue_requirements", columnDefinition = "TEXT")
    private String venueRequirements;

    @Column(name = "technical_requirements", columnDefinition = "TEXT")
    private String technicalRequirements;

    @Column(name = "accessibility_features", columnDefinition = "TEXT")
    private String accessibilityFeatures;

    @Column(name = "emergency_plan", columnDefinition = "TEXT")
    private String emergencyPlan;

    @Column(name = "backup_plan", columnDefinition = "TEXT")
    private String backupPlan;

    @Column(name = "post_event_tasks", columnDefinition = "TEXT")
    private String postEventTasks;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "venue_id")
    private UUID venueId;
    
    // Platform payment tracking
    @Column(name = "platform_payment_id")
    private UUID platformPaymentId;
    
    @Column(name = "creation_fee_paid")
    private Boolean creationFeePaid = false;
    
    @Column(name = "creation_fee_amount", precision = 10, scale = 2)
    private java.math.BigDecimal creationFeeAmount;
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    // Archive/restore fields
    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archived_by")
    private UUID archivedBy;

    @Column(name = "archive_reason", columnDefinition = "TEXT")
    private String archiveReason;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    @Column(name = "restored_by")
    private UUID restoredBy;

    public Event(String name, EventType eventType, UUID ownerId) {
        this.name = name;
        this.eventType = eventType;
        this.ownerId = ownerId;
    }
}
