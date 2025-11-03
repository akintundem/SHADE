package ai.eventplanner.timeline.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import ai.eventplanner.common.domain.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event timeline and run-of-show management
 */
@Entity
@Table(name = "timeline_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TimelineItem extends BaseEntity {
    
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    private ItemType itemType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.PENDING;
    
    @Column(name = "priority")
    private String priority = "MEDIUM";
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;
    
    @Column(name = "assigned_to_organization_id")
    private UUID assignedToOrganizationId;
    
    @Column(name = "dependencies", columnDefinition = "TEXT")
    private String dependencies; // Array of timeline_item IDs
    
    @Column(name = "setup_time_minutes")
    private Integer setupTimeMinutes;
    
    @Column(name = "teardown_time_minutes")
    private Integer teardownTimeMinutes;
    
    @Column(name = "resources_required", columnDefinition = "TEXT")
    private String resourcesRequired;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    // Task hierarchy support
    @Column(name = "parent_task_id")
    private UUID parentTaskId;
    
    @Column(name = "task_order")
    private Integer taskOrder; // For ordering subtasks
    
    @Column(name = "is_parent_task")
    private Boolean isParentTask = false;
    
    // Progress tracking
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0; // 0-100
    
    @Column(name = "completed_subtasks_count")
    private Integer completedSubtasksCount = 0;
    
    @Column(name = "total_subtasks_count")
    private Integer totalSubtasksCount = 0;
    
    // Category and additional fields
    @Column(name = "category")
    private String category; // LOGISTICS, MARKETING, CATERING, etc.
    
    @Column(name = "start_date")
    private LocalDateTime startDate; // Explicit start date for timeline visualization
    
    @Column(name = "due_date")
    private LocalDateTime dueDate; // Due date (may differ from endTime)
    
    @Column(name = "is_preview")
    private Boolean isPreview = false; // For "Preview" state in timeline
    
    @Column(name = "color_code")
    private String colorCode; // For UI visualization
    
    // Payment and proof fields
    @Column(name = "payment_date")
    private LocalDateTime paymentDate; // When payment was made for this task
    
    @Column(name = "proof_image_url")
    private String proofImageUrl; // URL to uploaded proof image of work done
    
    @Column(name = "proof_image_urls", columnDefinition = "TEXT")
    private String proofImageUrls; // JSON array of multiple proof image URLs
    
    public TimelineItem(UUID eventId, String title, LocalDateTime scheduledAt, Integer durationMinutes) {
        this.eventId = eventId;
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.startDate = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.endTime = scheduledAt != null && durationMinutes != null 
            ? scheduledAt.plusMinutes(durationMinutes) 
            : null;
    }
    
    public enum ItemType {
        SETUP,
        REGISTRATION,
        WELCOME,
        PRESENTATION,
        BREAK,
        NETWORKING,
        MEAL,
        ENTERTAINMENT,
        AWARDS,
        CLOSING,
        TEARDOWN,
        OTHER
    }
}
