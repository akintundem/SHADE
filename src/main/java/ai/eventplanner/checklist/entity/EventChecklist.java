package ai.eventplanner.checklist.entity;

import ai.eventplanner.event.entity.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event checklist and task management.
 */
@Entity
@Table(name = "event_checklists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.PENDING;

    @Column(name = "priority")
    private String priority = "MEDIUM";

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_essential")
    private Boolean isEssential = false;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Category {
        PLANNING,
        VENUE,
        VENDORS,
        BUDGET,
        MARKETING,
        REGISTRATION,
        LOGISTICS,
        TECHNICAL,
        SECURITY,
        CATERING,
        DECORATIONS,
        ENTERTAINMENT,
        COMMUNICATIONS,
        RISK_MANAGEMENT,
        POST_EVENT,
        OTHER
    }

    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        ON_HOLD
    }
}
