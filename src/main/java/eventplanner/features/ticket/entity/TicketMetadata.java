package eventplanner.features.ticket.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Ticket metadata as key-value pairs - normalizes tickets.metadata TEXT field.
 */
@Entity
@Table(name = "ticket_metadata",
    uniqueConstraints = @UniqueConstraint(name = "uk_ticket_metadata", columnNames = {"ticket_id", "metadata_key"}),
    indexes = {
        @Index(name = "idx_ticket_metadata_ticket", columnList = "ticket_id"),
        @Index(name = "idx_ticket_metadata_key", columnList = "metadata_key")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketMetadata extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ticket_metadata_ticket"))
    private Ticket ticket;

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
