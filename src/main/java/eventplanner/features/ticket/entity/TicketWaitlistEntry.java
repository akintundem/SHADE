package eventplanner.features.ticket.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.event.entity.Event;
import eventplanner.features.ticket.enums.TicketWaitlistStatus;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "ticket_waitlist_entries",
    indexes = {
        @Index(name = "idx_ticket_waitlist_event_id", columnList = "event_id"),
        @Index(name = "idx_ticket_waitlist_ticket_type_id", columnList = "ticket_type_id"),
        @Index(name = "idx_ticket_waitlist_status", columnList = "status"),
        @Index(name = "idx_ticket_waitlist_requester", columnList = "requester_user_id"),
        @Index(name = "idx_ticket_waitlist_event_status", columnList = "event_id, status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketWaitlistEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

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
    private TicketWaitlistStatus status = TicketWaitlistStatus.WAITING;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 50, message = "Quantity must not exceed 50")
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfilled_by")
    private UserAccount fulfilledBy;

    @Column(name = "fulfilled_at")
    private LocalDateTime fulfilledAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    @PreUpdate
    public void validateRequester() {
        if (status == null) {
            status = TicketWaitlistStatus.WAITING;
        }
        if (quantity == null) {
            quantity = 1;
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

    public void fulfill(UserAccount approver) {
        ensureWaiting();
        status = TicketWaitlistStatus.FULFILLED;
        fulfilledAt = LocalDateTime.now();
        fulfilledBy = approver;
    }

    public void cancel() {
        ensureWaiting();
        status = TicketWaitlistStatus.CANCELLED;
        cancelledAt = LocalDateTime.now();
    }

    private void ensureWaiting() {
        if (status != TicketWaitlistStatus.WAITING) {
            throw new IllegalStateException("Waitlist entry is not waiting");
        }
    }
}
