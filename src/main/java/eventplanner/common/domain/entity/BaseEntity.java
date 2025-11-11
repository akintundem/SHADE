package eventplanner.common.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base entity with common fields for all entities
 * Includes soft delete support via deletedAt field
 */
@MappedSuperclass
@SQLDelete(sql = "UPDATE #{#entityName} SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Version
    private Long version;
    
    /**
     * Soft delete this entity
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
    
    /**
     * Check if this entity is soft deleted
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
    
    /**
     * Restore a soft deleted entity
     */
    public void restore() {
        this.deletedAt = null;
    }
}
