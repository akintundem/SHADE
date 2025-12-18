package eventplanner.features.budget.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Budget Category - represents a category within a budget with its own allocated amount
 * Example: Marketing, Catering, Venue, etc.
 * Line items are created under these categories.
 */
@Entity
@Table(name = "budget_categories", indexes = {
    @Index(name = "idx_budget_categories_budget", columnList = "budget_id"),
    @Index(name = "idx_budget_categories_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@org.hibernate.annotations.SQLDelete(sql = "UPDATE budget_categories SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
public class BudgetCategory extends BaseEntity {

    /**
     * Many-to-one relationship with the budget this category belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "budget_id", 
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_budget_categories_budget", value = ConstraintMode.CONSTRAINT)
    )
    private Budget budget;

    /**
     * Category name (e.g., "Marketing", "Catering", "Venue & Facilities")
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Description of what this category covers
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Allocated budget amount for this category
     */
    @Column(name = "allocated_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal allocatedAmount;

    /**
     * Total estimated cost from all line items in this category
     */
    @Column(name = "total_estimated", precision = 12, scale = 2)
    private BigDecimal totalEstimated = BigDecimal.ZERO;

    /**
     * Total actual cost from all line items in this category
     */
    @Column(name = "total_actual", precision = 12, scale = 2)
    private BigDecimal totalActual = BigDecimal.ZERO;

    /**
     * Remaining budget in this category (allocatedAmount - totalActual)
     */
    @Column(name = "remaining", precision = 12, scale = 2)
    private BigDecimal remaining;

    /**
     * Display order for sorting categories
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * Line items within this category
     */
    @OneToMany(mappedBy = "budgetCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetLineItem> lineItems = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (totalEstimated == null) totalEstimated = BigDecimal.ZERO;
        if (totalActual == null) totalActual = BigDecimal.ZERO;
        if (remaining == null) remaining = allocatedAmount;
        if (displayOrder == null) displayOrder = 0;
    }

    @PreUpdate
    public void preUpdate() {
        // Calculate remaining budget
        if (allocatedAmount != null && totalActual != null) {
            remaining = allocatedAmount.subtract(totalActual);
        }
    }
}

