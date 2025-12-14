package eventplanner.features.timeline.entity;

import eventplanner.common.domain.enums.TimelineStatus;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "timeline_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimelineItem {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Many-to-one relationship with the event this timeline item belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /**
     * Many-to-one relationship with the user assigned to this timeline item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private UserAccount assignedTo;

    @Column(name = "dependencies")
    private UUID[] dependencies;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TimelineStatus status;
    
    // Task hierarchy support
    /**
     * Self-referential many-to-one relationship for task hierarchy (parent task).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private TimelineItem parentTask;
    
    @Column(name = "task_order")
    private Integer taskOrder;
    
    @Column(name = "is_parent_task")
    private Boolean isParentTask = false;
    
    // Progress tracking
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;
    
    @Column(name = "completed_subtasks_count")
    private Integer completedSubtasksCount = 0;
    
    @Column(name = "total_subtasks_count")
    private Integer totalSubtasksCount = 0;
    
    // Category and timeline fields
    @Column(name = "category")
    private String category;
    
    @Column(name = "priority")
    private String priority = "MEDIUM";
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "is_preview")
    private Boolean isPreview = false;
    
    // Payment and proof fields
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
    
    @Column(name = "proof_image_url")
    private String proofImageUrl;
    
    @Column(name = "proof_image_urls", columnDefinition = "TEXT")
    private String proofImageUrls;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = TimelineStatus.PENDING;
        if (isParentTask == null) isParentTask = false;
        if (progressPercentage == null) progressPercentage = 0;
        if (completedSubtasksCount == null) completedSubtasksCount = 0;
        if (totalSubtasksCount == null) totalSubtasksCount = 0;
        if (isPreview == null) isPreview = false;
        if (priority == null) priority = "MEDIUM";
    }

}


