package eventplanner.features.budget.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.domain.enums.PlanningStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "budget_line_items", indexes = {
    @Index(name = "idx_budget_line_items_budget_category", columnList = "budget_category_id"),
    @Index(name = "idx_budget_line_items_budget", columnList = "budget_id"),
    @Index(name = "idx_budget_line_items_subcategory", columnList = "subcategory"),
    @Index(name = "idx_budget_line_items_is_draft", columnList = "is_draft")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@org.hibernate.annotations.SQLDelete(sql = "UPDATE budget_line_items SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
public class BudgetLineItem extends BaseEntity {

    /**
     * Many-to-one relationship with the budget this line item belongs to.
     * Foreign key constraint with ON DELETE CASCADE ensures line items are deleted
     * when the budget is deleted at the database level.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "budget_id", 
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_budget_line_items_budget", value = ConstraintMode.CONSTRAINT)
    )
    private Budget budget;

    /**
     * Many-to-one relationship with the budget category this line item belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "budget_category_id", 
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_budget_line_items_category", value = ConstraintMode.CONSTRAINT)
    )
    private BudgetCategory budgetCategory;

    @Column(name = "subcategory")
    private String subcategory;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 10, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "variance", precision = 10, scale = 2)
    private BigDecimal variance;

    @Column(name = "variance_percentage", precision = 5, scale = 2)
    private BigDecimal variancePercentage;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "planning_status")
    private PlanningStatus planningStatus;

    @Column(name = "is_essential")
    private Boolean isEssential;

    @Column(name = "priority")
    private String priority;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Indicates if this line item is a draft (auto-saved) or finalized.
     * Drafts are saved without full validation and don't trigger budget recalculation.
     */
    @Column(name = "is_draft", nullable = false)
    private Boolean isDraft = false;

    @PrePersist
    public void prePersist() {
        if (quantity == null) quantity = 1;
        if (planningStatus == null) planningStatus = PlanningStatus.PLANNED;
        if (isEssential == null) isEssential = false;
        if (priority == null) priority = "MEDIUM";
        if (isDraft == null) isDraft = false;
    }
}


