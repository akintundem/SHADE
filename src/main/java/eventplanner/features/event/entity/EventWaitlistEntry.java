package eventplanner.features.event.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.event.enums.EventWaitlistStatus;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a waitlist entry for an event when it reaches capacity.
 * Users can join the waitlist and will be automatically promoted when spots become available.
 */
@Entity
@Table(
    name = "event_waitlist_entries",
    indexes = {
        @Index(name = "idx_event_waitlist_event_id", columnList = "event_id"),
        @Index(name = "idx_event_waitlist_status", columnList = "status"),
        @Index(name = "idx_event_waitlist_requester", columnList = "requester_user_id"),
        @Index(name = "idx_event_waitlist_event_status", columnList = "event_id, status"),
        @Index(name = "idx_event_waitlist_event_status_created", columnList = "event_id, status, created_at"),
        @Index(name = "idx_event_waitlist_created_at", columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventWaitlistEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id")
    private UserAccount requester;

    @Email(message = "Email must be valid if provided")
    @Size(max = 180, message = "Email must not exceed 180 characters")
    @Column(name = "requester_email", length = 180)
    private String requesterEmail;

    @Size(max = 200, message = "Name must not exceed 200 characters")
    @Column(name = "requester_name", length = 200)
    private String requesterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EventWaitlistStatus status = EventWaitlistStatus.WAITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promoted_by")
    private UserAccount promotedBy;

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    @PreUpdate
    public void validateRequester() {
        if (status == null) {
            status = EventWaitlistStatus.WAITING;
        }
        if (requester == null) {
            if (requesterEmail == null || requesterEmail.trim().isEmpty()) {
                throw new IllegalStateException("Waitlist entry must have requester email");
            }
            if (requesterName == null || requesterName.trim().isEmpty()) {
                throw new IllegalStateException("Waitlist entry must have requester name");
            }
        }
    }

    /**
     * Promote this waitlist entry when capacity becomes available.
     */
    public void promote(UserAccount approver) {
        ensureWaiting();
        status = EventWaitlistStatus.PROMOTED;
        promotedAt = LocalDateTime.now();
        promotedBy = approver;
    }

    /**
     * Cancel this waitlist entry.
     */
    public void cancel() {
        ensureWaiting();
        status = EventWaitlistStatus.CANCELLED;
        cancelledAt = LocalDateTime.now();
    }

    private void ensureWaiting() {
        if (status != EventWaitlistStatus.WAITING) {
            throw new IllegalStateException("Waitlist entry is not waiting");
        }
    }

    public UUID getEventId() {
        return event != null ? event.getId() : null;
    }

    public UUID getRequesterId() {
        return requester != null ? requester.getId() : null;
    }
}
