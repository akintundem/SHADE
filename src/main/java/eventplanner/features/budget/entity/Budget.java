package eventplanner.features.budget.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Budget {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "total_budget", nullable = false)
    private BigDecimal totalBudget;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "contingency_percentage", precision = 5, scale = 2)
    private BigDecimal contingencyPercentage;

    @Column(name = "contingency_amount", precision = 12, scale = 2)
    private BigDecimal contingencyAmount;

    @Column(name = "total_estimated", precision = 12, scale = 2)
    private BigDecimal totalEstimated;

    @Column(name = "total_actual", precision = 12, scale = 2)
    private BigDecimal totalActual;

    @Column(name = "variance", precision = 12, scale = 2)
    private BigDecimal variance;

    @Column(name = "variance_percentage", precision = 5, scale = 2)
    private BigDecimal variancePercentage;

    @Column(name = "budget_status")
    private String budgetStatus;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "budgetId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BudgetLineItem> lineItems;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (currency == null) currency = "USD";
        if (totalBudget == null) totalBudget = BigDecimal.ZERO;
        if (contingencyPercentage == null) contingencyPercentage = new BigDecimal("10.00");
        if (budgetStatus == null) budgetStatus = "DRAFT";
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}


