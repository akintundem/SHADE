package eventplanner.features.ticket.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Line item within a ticket checkout session.
 */
@Entity
@Table(name = "ticket_checkout_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
public class TicketCheckoutItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private TicketCheckout checkout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private TicketType ticketType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_minor")
    private Long unitPriceMinor = 0L;

    @Column(name = "subtotal_minor")
    private Long subtotalMinor = 0L;

    @Column(name = "currency", length = 3)
    private String currency;
}
