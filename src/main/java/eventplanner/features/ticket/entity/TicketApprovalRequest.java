package eventplanner.features.ticket.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.event.entity.Event;
import eventplanner.features.ticket.enums.TicketApprovalStatus;
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
    name = "ticket_approval_requests",
    indexes = {
        @Index(name = "idx_ticket_approval_event_id", columnList = "event_id"),
        @Index(name = "idx_ticket_approval_ticket_type_id", columnList = "ticket_type_id"),
        @Index(name = "idx_ticket_approval_status", columnList = "status"),
        @Index(name = "idx_ticket_approval_requester", columnList = "requester_user_id"),
        @Index(name = "idx_ticket_approval_event_status", columnList = "event_id, status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketApprovalRequest extends BaseEntity {

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
    private TicketApprovalStatus status = TicketApprovalStatus.PENDING;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 50, message = "Quantity must not exceed 50")
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private UserAccount decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decision_note", columnDefinition = "TEXT")
    private String decisionNote;

    @PrePersist
    @PreUpdate
    public void validateRequester() {
        if (status == null) {
            status = TicketApprovalStatus.PENDING;
        }
        if (quantity == null) {
            quantity = 1;
        }
        if (requester == null) {
            if (requesterEmail == null || requesterEmail.trim().isEmpty()) {
                throw new IllegalStateException("Approval request must have requester email");
            }
            if (requesterName == null || requesterName.trim().isEmpty()) {
                throw new IllegalStateException("Approval request must have requester name");
            }
        }
    }

    public void approve(UserAccount approver) {
        ensurePending();
        status = TicketApprovalStatus.APPROVED;
        decidedAt = LocalDateTime.now();
        decidedBy = approver;
    }

    public void reject(UserAccount approver, String note) {
        ensurePending();
        status = TicketApprovalStatus.REJECTED;
        decidedAt = LocalDateTime.now();
        decidedBy = approver;
        decisionNote = note;
    }

    public void cancel() {
        ensurePending();
        status = TicketApprovalStatus.CANCELLED;
        decidedAt = LocalDateTime.now();
    }

    private void ensurePending() {
        if (status != TicketApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval request is not pending");
        }
    }
}
