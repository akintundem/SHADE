package eventplanner.security.authorization.domain.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Organization role assignment for users
 */
@Entity
@Table(
    name = "organization_roles",
    indexes = {
        @Index(name = "idx_org_roles_user_org", columnList = "userId, organizationId"),
        @Index(name = "idx_org_roles_role", columnList = "role")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class OrganizationRole extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "role", nullable = false, length = 30)
    private String role; // OWNER, MANAGER, MEMBER

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private java.time.LocalDateTime assignedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
