package eventplanner.features.event.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event metadata as key-value pairs - normalizes events.metadata JSON field.
 */
@Entity
@Table(name = "event_metadata",
    uniqueConstraints = @UniqueConstraint(name = "uk_event_metadata", columnNames = {"event_id", "metadata_key"}),
    indexes = {
        @Index(name = "idx_event_metadata_event", columnList = "event_id"),
        @Index(name = "idx_event_metadata_key", columnList = "metadata_key")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventMetadata extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_metadata_event"))
    private Event event;

    /**
     * Metadata key
     */
    @Column(name = "metadata_key", nullable = false, length = 100)
    private String metadataKey;

    /**
     * Metadata value (stored as text, can be parsed as needed)
     */
    @Column(name = "metadata_value", columnDefinition = "TEXT")
    private String metadataValue;

    /**
     * Data type hint (STRING, NUMBER, BOOLEAN, JSON, DATE)
     */
    @Column(name = "metadata_type", length = 20)
    private String metadataType = "STRING";
}
