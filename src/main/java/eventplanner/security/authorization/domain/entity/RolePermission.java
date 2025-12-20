package eventplanner.security.authorization.domain.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.domain.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Role-Permission mapping for authorization
 */
@Entity
@Table(name = "role_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class RolePermission extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false)
    private RoleName roleName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "context", nullable = false, length = 20)
    private String context; // "system", "event"

    @Column(name = "is_granted", nullable = false)
    @Builder.Default
    private Boolean isGranted = true;

    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions; // JSON string for additional conditions
}
