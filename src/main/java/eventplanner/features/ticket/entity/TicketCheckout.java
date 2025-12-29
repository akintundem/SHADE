package eventplanner.features.ticket.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.features.ticket.enums.TicketCheckoutStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Checkout session for ticket purchases.
 * Holds reserved tickets, cost breakdown, and lifecycle status so payment can be plugged in later.
 */
@Entity
@Table(name = "ticket_checkouts", indexes = {
    @Index(name = "idx_ticket_checkout_event_status", columnList = "event_id, status"),
    @Index(name = "idx_ticket_checkout_expires_at", columnList = "expires_at"),
    @Index(name = "idx_ticket_checkout_purchaser", columnList = "purchaser_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
public class TicketCheckout extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchaser_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private UserAccount purchaser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TicketCheckoutStatus status = TicketCheckoutStatus.PENDING_PAYMENT;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD";

    @Column(name = "subtotal_minor")
    private Long subtotalMinor = 0L;

    @Column(name = "fees_minor")
    private Long feesMinor = 0L;

    @Column(name = "tax_minor")
    private Long taxMinor = 0L;

    @Column(name = "discount_minor")
    private Long discountMinor = 0L;

    @Column(name = "total_minor")
    private Long totalMinor = 0L;

    @Column(name = "applied_promotion_code", length = 80)
    private String appliedPromotionCode;

    @Column(name = "applied_discount_minor")
    private Long appliedDiscountMinor = 0L;

    /**
     * Ticket line items connected to this checkout.
     */
    @OneToMany(mappedBy = "checkout", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<TicketCheckoutItem> items = new ArrayList<>();

    /**
     * Tickets generated for this checkout (remain PENDING until checkout completes).
     */
    @OneToMany(mappedBy = "checkout", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<Ticket> tickets = new ArrayList<>();
}
