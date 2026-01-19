package eventplanner.features.collaboration.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.collaboration.enums.EventPermission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Permission entry linked to an EventUser membership.
 */
@Entity
@Table(name = "event_user_permissions", indexes = {
    @Index(name = "idx_event_user_permissions_user", columnList = "event_user_id"),
    @Index(name = "idx_event_user_permissions_permission", columnList = "permission")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventUserPermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_user_permissions_user"))
    private EventUser eventUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 120)
    private EventPermission permission;
}
