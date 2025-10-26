package ai.eventplanner.auth.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import ai.eventplanner.common.domain.enums.ActionType;
import ai.eventplanner.common.domain.enums.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Permission entity for fine-grained access control using enums
 */
@Entity
@Table(
    name = "permissions",
    indexes = {
        @Index(name = "idx_permissions_name", columnList = "name"),
        @Index(name = "idx_permissions_resource", columnList = "resource"),
        @Index(name = "idx_permissions_action", columnList = "action")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name; // e.g., "event.create", "budget.manage"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ResourceType resource; // e.g., EVENT, BUDGET, USER, ORGANIZATION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActionType action; // e.g., CREATE, READ, UPDATE, DELETE, MANAGE

    @Column(name = "is_system_permission", nullable = false)
    @Builder.Default
    private Boolean isSystemPermission = false;

    @Column(name = "is_organization_permission", nullable = false)
    @Builder.Default
    private Boolean isOrganizationPermission = false;

    @Column(name = "is_event_permission", nullable = false)
    @Builder.Default
    private Boolean isEventPermission = false;
    
    /**
     * Generate permission name from resource and action
     */
    public static String generatePermissionName(ResourceType resource, ActionType action) {
        return resource.name().toLowerCase() + "." + action.name().toLowerCase();
    }
    
    /**
     * Get permission name
     */
    public String getPermissionName() {
        return generatePermissionName(resource, action);
    }
}
