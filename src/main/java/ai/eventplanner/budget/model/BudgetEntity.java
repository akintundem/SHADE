package ai.eventplanner.budget.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "total_budget", nullable = false)
    private BigDecimal totalBudget;

    @Column(name = "currency", length = 3)
    private String currency;

    @OneToMany(mappedBy = "budgetId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BudgetLineItemEntity> lineItems;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (currency == null) currency = "USD";
        if (totalBudget == null) totalBudget = BigDecimal.ZERO;
    }

}


