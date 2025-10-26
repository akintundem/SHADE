package ai.eventplanner.timeline.model;

import ai.eventplanner.common.domain.enums.Status;
import ai.eventplanner.timeline.entity.TimelineItem;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
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

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    private TimelineItem.ItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "priority")
    private String priority;

    @Column(name = "location")
    private String location;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @ElementCollection
    @CollectionTable(name = "timeline_dependencies", joinColumns = @JoinColumn(name = "timeline_item_id"))
    @Column(name = "dependency_id")
    private List<UUID> dependencies;

    @Column(name = "setup_time_minutes")
    private Integer setupTimeMinutes;

    @Column(name = "teardown_time_minutes")
    private Integer teardownTimeMinutes;

    @Column(name = "resources_required")
    private String resourcesRequired;

    @Column(name = "notes")
    private String notes;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = Status.PENDING;
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (scheduledAt != null && durationMinutes != null) {
            endTime = scheduledAt.plusMinutes(durationMinutes);
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (scheduledAt != null && durationMinutes != null) {
            endTime = scheduledAt.plusMinutes(durationMinutes);
        }
    }
}


