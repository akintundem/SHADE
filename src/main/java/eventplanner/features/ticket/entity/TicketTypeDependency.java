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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "ticket_type_dependencies",
    indexes = {
        @Index(name = "idx_ticket_dependency_type", columnList = "ticket_type_id"),
        @Index(name = "idx_ticket_dependency_required", columnList = "required_ticket_type_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketTypeDependency extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_ticket_type_id", nullable = false)
    private TicketType requiredTicketType;

    @Min(value = 1, message = "Minimum quantity must be at least 1")
    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity = 1;
}
