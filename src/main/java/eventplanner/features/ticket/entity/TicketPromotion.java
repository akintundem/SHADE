package eventplanner.features.ticket.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.event.entity.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Promotion/discount code scoped to an event.
 */
@Entity
@Table(name = "ticket_promotions", indexes = {
    @Index(name = "idx_ticket_promotions_event_code", columnList = "event_id, code", unique = true),
    @Index(name = "idx_ticket_promotions_active", columnList = "event_id, is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketPromotion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @NotBlank
    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    /**
     * Percentage off in basis points (e.g., 1500 = 15%).
     */
    @Min(0)
    @Max(100_00)
    @Column(name = "percent_off_basis_points")
    private Integer percentOffBasisPoints;

    /**
     * Fixed amount off in minor units (e.g., cents).
     */
    @Min(0)
    @Column(name = "amount_off_minor")
    private Long amountOffMinor;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public boolean isValidNow() {
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        if (startsAt != null && now.isBefore(startsAt)) {
            return false;
        }
        if (endsAt != null && now.isAfter(endsAt)) {
            return false;
        }
        return true;
    }
}
