package ai.eventplanner.budget.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event budget management
 */
@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Budget extends BaseEntity {
    
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    
    @Column(name = "total_budget", precision = 12, scale = 2)
    private BigDecimal totalBudget;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Column(name = "contingency_percentage", precision = 5, scale = 2)
    private BigDecimal contingencyPercentage = new BigDecimal("10.00");
    
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
    private String budgetStatus = "DRAFT";
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    // Relationships
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BudgetLineItem> lineItems;
    
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BudgetRevenue> revenues;
    
    public Budget(UUID eventId, BigDecimal totalBudget) {
        this.eventId = eventId;
        this.totalBudget = totalBudget;
    }
}
