package eventplanner.features.ticket.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.ticket.enums.TicketStatus;
import eventplanner.features.event.entity.Event;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ticket entity representing an individual ticket issued to an attendee.
 * Each ticket has a unique ticket number and QR code for validation.
 */
@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_tickets_ticket_number", columnList = "ticket_number", unique = true),
    @Index(name = "idx_tickets_event_id", columnList = "event_id"),
    @Index(name = "idx_tickets_attendee_id", columnList = "attendee_id"),
    @Index(name = "idx_tickets_ticket_type_id", columnList = "ticket_type_id"),
    @Index(name = "idx_tickets_status", columnList = "status"),
    @Index(name = "idx_tickets_qr_code_data", columnList = "qr_code_data"),
    @Index(name = "idx_tickets_event_status", columnList = "event_id, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Ticket extends BaseEntity {

    /**
     * Unique ticket number for identification.
     */
    @NotBlank(message = "Ticket number is required")
    @Size(max = 50, message = "Ticket number must not exceed 50 characters")
    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    /**
     * Many-to-one relationship with the event this ticket belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Many-to-one relationship with the ticket type.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    /**
     * Many-to-one relationship with the attendee this ticket is issued to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendee_id")
    private Attendee attendee;

    /**
     * Email address of the ticket owner (for tickets owned by email, not attendee).
     * Required if attendee is null.
     */
    @Email(message = "Email must be valid if provided")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    @Column(name = "owner_email", length = 254)
    private String ownerEmail;

    /**
     * Name of the ticket owner (for tickets owned by email, not attendee).
     * Required if attendee is null.
     */
    @Size(max = 200, message = "Name must not exceed 200 characters")
    @Column(name = "owner_name", length = 200)
    private String ownerName;

    @NotNull(message = "Ticket status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;

    /**
     * QR code data string for validation.
     * Format: ticket:{ticketId}:{ticketNumber}:{eventId}:{hash}
     */
    @NotBlank(message = "QR code data is required")
    @Column(name = "qr_code_data", nullable = false, columnDefinition = "TEXT")
    private String qrCodeData;

    /**
     * Base64 encoded QR code image for immediate display.
     * Optional - can be generated on demand.
     */
    @Column(name = "qr_code_image_base64", columnDefinition = "TEXT")
    private String qrCodeImageBase64;

    /**
     * URL to QR code image if stored externally.
     */
    @Size(max = 500, message = "QR code image URL must not exceed 500 characters")
    @Column(name = "qr_code_image_url", length = 500)
    private String qrCodeImageUrl;

    @Column(name = "pending_at")
    private LocalDateTime pendingAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Payment ID for future payment gateway integration.
     * NULL for free tickets or pending payments.
     */
    @Column(name = "payment_id")
    private UUID paymentId;

    /**
     * User who issued this ticket.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private UserAccount issuedBy;

    /**
     * User who validated this ticket (scanned QR code).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private UserAccount validatedBy;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    /**
     * Metadata stored as JSON string for extensibility.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = TicketStatus.PENDING;
        }
        // Set pending timestamp when ticket is created in PENDING status
        if (status == TicketStatus.PENDING && pendingAt == null) {
            pendingAt = LocalDateTime.now();
        }
        // Validate that either attendee or email/name is provided
        validateOwner();
    }

    /**
     * Validate that ticket has either an attendee or email/name for email-only ownership.
     */
    private void validateOwner() {
        if (attendee == null) {
            if (ownerEmail == null || ownerEmail.trim().isEmpty()) {
                throw new IllegalStateException("Ticket must have either an attendee or owner email");
            }
            if (ownerName == null || ownerName.trim().isEmpty()) {
                throw new IllegalStateException("Ticket must have owner name when attendee is not provided");
            }
        }
    }

    /**
     * Issue this ticket (transition from PENDING to ISSUED).
     * 
     * @param issuedBy User who is issuing the ticket
     * @throws IllegalStateException if ticket is not in PENDING status or has expired
     */
    public void issue(UserAccount issuedBy) {
        if (status != TicketStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot issue ticket. Current status: " + status + ". Expected: PENDING");
        }
        
        // Check if pending ticket has expired (15 minute window)
        if (isPendingExpired()) {
            throw new IllegalStateException(
                "Cannot issue ticket. Pending reservation has expired (15 minute window exceeded)");
        }
        
        this.status = TicketStatus.ISSUED;
        this.issuedAt = LocalDateTime.now();
        this.issuedBy = issuedBy;
        this.pendingAt = null; // Clear pending timestamp
    }

    /**
     * Validate this ticket (transition from ISSUED to VALIDATED).
     * Also updates the attendee's checked-in status.
     * 
     * @param validatedBy User who is validating the ticket
     * @throws IllegalStateException if ticket is not in ISSUED status
     */
    public void validate(UserAccount validatedBy) {
        if (status != TicketStatus.ISSUED) {
            throw new IllegalStateException(
                "Cannot validate ticket. Current status: " + status + ". Expected: ISSUED");
        }
        
        this.status = TicketStatus.VALIDATED;
        this.validatedAt = LocalDateTime.now();
        this.validatedBy = validatedBy;
        
        // Update attendee check-in status if attendee exists
        if (attendee != null && attendee.getCheckedInAt() == null) {
            attendee.setCheckedInAt(LocalDateTime.now());
        }
        // Note: For email-only tickets, check-in is tracked via validatedAt timestamp
    }

    /**
     * Cancel this ticket.
     * 
     * @param reason Reason for cancellation (optional)
     * @throws IllegalStateException if ticket is already validated or refunded
     */
    public void cancel(String reason) {
        if (status == TicketStatus.VALIDATED) {
            throw new IllegalStateException("Cannot cancel a validated ticket");
        }
        if (status == TicketStatus.REFUNDED) {
            throw new IllegalStateException("Cannot cancel a refunded ticket");
        }
        if (status == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already cancelled");
        }
        
        this.status = TicketStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }

    /**
     * Check if ticket can be validated.
     * 
     * @return true if ticket is in ISSUED status and not expired
     */
    public boolean canBeValidated() {
        if (status != TicketStatus.ISSUED) {
            return false;
        }
        
        // Check if event has passed (optional validation)
        if (event != null && event.getStartDateTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            // Allow validation up to 24 hours after event start
            if (now.isAfter(event.getStartDateTime().plusHours(24))) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Check if ticket is expired (past event date).
     * 
     * @return true if event has passed
     */
    public boolean isExpired() {
        if (event == null || event.getStartDateTime() == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        // Consider expired if more than 24 hours after event start
        return now.isAfter(event.getStartDateTime().plusHours(24));
    }

    /**
     * Check if a pending ticket has expired (15 minute window).
     * 
     * @return true if ticket is in PENDING status and more than 15 minutes have passed
     */
    public boolean isPendingExpired() {
        if (status != TicketStatus.PENDING || pendingAt == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        // Pending tickets expire after 15 minutes
        return now.isAfter(pendingAt.plusMinutes(15));
    }

    /**
     * Get the expiration time for pending tickets.
     * 
     * @return LocalDateTime when the pending ticket expires, or null if not pending
     */
    public LocalDateTime getPendingExpirationTime() {
        if (status != TicketStatus.PENDING || pendingAt == null) {
            return null;
        }
        return pendingAt.plusMinutes(15);
    }
}
