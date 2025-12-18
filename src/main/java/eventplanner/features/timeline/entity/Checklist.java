package eventplanner.features.timeline.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.domain.enums.TimelineStatus;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "checklists")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Checklist extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TimelineStatus status = TimelineStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private UserAccount assignedTo;

    @Column(name = "task_order")
    private Integer taskOrder;

    @Builder.Default
    @Column(name = "is_draft")
    private Boolean isDraft = true;

    @PrePersist
    public void prePersist() {
        if (getId() == null) setId(UUID.randomUUID());
        if (status == null) status = TimelineStatus.PENDING;
        if (isDraft == null) isDraft = true;
    }
}

