package eventplanner.features.ticket.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "ticket_price_tiers",
    indexes = {
        @Index(name = "idx_ticket_price_tier_type", columnList = "ticket_type_id"),
        @Index(name = "idx_ticket_price_tier_dates", columnList = "starts_at, ends_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketPriceTier extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @Size(max = 120, message = "Tier name must not exceed 120 characters")
    @Column(name = "name", length = 120)
    private String name;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Min(value = 0, message = "Tier price must be greater than or equal to 0")
    @Column(name = "price_minor", nullable = false)
    private Long priceMinor;

    @Column(name = "priority")
    private Integer priority = 0;
}
