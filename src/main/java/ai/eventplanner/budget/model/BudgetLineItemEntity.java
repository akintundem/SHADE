package ai.eventplanner.budget.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
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

    @Column(name = "description")
    private String description;

    @Column(name = "estimated_cost")
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost")
    private BigDecimal actualCost;

    @Column(name = "vendor_id")
    private UUID vendorId;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }

}


