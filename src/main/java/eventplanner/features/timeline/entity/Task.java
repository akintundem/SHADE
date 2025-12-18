package eventplanner.features.timeline.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.domain.enums.TimelineStatus;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Task extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Builder.Default
    @Column(name = "priority")
    private String priority = "MEDIUM";

    @Column(name = "category")
    private String category;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TimelineStatus status = TimelineStatus.PENDING;

    @Builder.Default
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private UserAccount assignedTo;

    @Column(name = "task_order")
    private Integer taskOrder;

    @Builder.Default
    @Column(name = "is_draft")
    private Boolean isDraft = true;

    @Builder.Default
    @Column(name = "completed_subtasks_count")
    private Integer completedSubtasksCount = 0;

    @Builder.Default
    @Column(name = "total_subtasks_count")
    private Integer totalSubtasksCount = 0;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Checklist> checklist = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null) setId(UUID.randomUUID());
        if (status == null) status = TimelineStatus.PENDING;
        if (progressPercentage == null) progressPercentage = 0;
        if (isDraft == null) isDraft = true;
        if (priority == null) priority = "MEDIUM";
        if (completedSubtasksCount == null) completedSubtasksCount = 0;
        if (totalSubtasksCount == null) totalSubtasksCount = 0;
    }
}

