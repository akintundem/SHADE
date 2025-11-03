package ai.eventplanner.timeline.model;

import ai.eventplanner.common.domain.enums.TimelineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
public class TimelineItemEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "dependencies")
    private UUID[] dependencies;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TimelineStatus status;
    
    // Task hierarchy support
    @Column(name = "parent_task_id")
    private UUID parentTaskId;
    
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


