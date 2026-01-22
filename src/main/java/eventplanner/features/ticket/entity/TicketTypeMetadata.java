package eventplanner.features.ticket.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Ticket type metadata as key-value pairs - normalizes ticket_types.metadata TEXT field.
 */
@Entity
@Table(name = "ticket_type_metadata",
    uniqueConstraints = @UniqueConstraint(name = "uk_ticket_type_metadata", columnNames = {"ticket_type_id", "metadata_key"}),
    indexes = {
        @Index(name = "idx_ticket_type_metadata_type", columnList = "ticket_type_id"),
        @Index(name = "idx_ticket_type_metadata_key", columnList = "metadata_key")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketTypeMetadata extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ticket_type_metadata_type"))
    private TicketType ticketType;

    /**
     * Metadata key
     */
    @Column(name = "metadata_key", nullable = false, length = 100)
    private String metadataKey;

    /**
     * Metadata value
     */
    @Column(name = "metadata_value", columnDefinition = "TEXT")
    private String metadataValue;
}
