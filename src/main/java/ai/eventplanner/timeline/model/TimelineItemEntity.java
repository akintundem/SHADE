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

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = TimelineStatus.PENDING;
    }

}


