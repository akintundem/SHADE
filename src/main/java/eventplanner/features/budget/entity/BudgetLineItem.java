package eventplanner.features.budget.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.domain.enums.PlanningStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_line_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BudgetLineItem extends BaseEntity {

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    @Column(name = "category", nullable = false)
    private String category;

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

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void prePersist() {
        if (quantity == null) quantity = 1;
        if (planningStatus == null) planningStatus = PlanningStatus.PLANNED;
        if (isEssential == null) isEssential = false;
        if (priority == null) priority = "MEDIUM";
    }
}


