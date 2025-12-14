package eventplanner.features.budget.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@org.hibernate.annotations.SQLDelete(sql = "UPDATE budgets SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
public class Budget extends BaseEntity {

    /**
     * Many-to-one relationship with the event this budget belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Many-to-one relationship with the user who owns this budget.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

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

    /**
     * One-to-many relationship with budget line items.
     * Lazy loaded to avoid N+1 queries.
     */
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BudgetLineItem> lineItems = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (currency == null) currency = "USD";
        if (totalBudget == null) totalBudget = BigDecimal.ZERO;
        if (contingencyPercentage == null) contingencyPercentage = new BigDecimal("10.00");
        if (budgetStatus == null) budgetStatus = "DRAFT";
    }
}


