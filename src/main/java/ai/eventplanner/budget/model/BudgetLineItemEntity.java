package ai.eventplanner.budget.model;

import ai.eventplanner.common.domain.enums.PlanningStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "budget_line_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetLineItemEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (quantity == null) quantity = 1;
        if (planningStatus == null) planningStatus = PlanningStatus.PLANNED;
        if (isEssential == null) isEssential = false;
        if (priority == null) priority = "MEDIUM";
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}


