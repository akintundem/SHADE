package ai.eventplanner.budget.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Budget revenue sources (ticket sales, sponsorships, etc.)
 */
@Entity
@Table(name = "budget_revenues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BudgetRevenue extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;
    
    @Column(name = "revenue_type")
    private String revenueType;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "estimated_amount", precision = 10, scale = 2)
    private BigDecimal estimatedAmount;
    
    @Column(name = "actual_amount", precision = 10, scale = 2)
    private BigDecimal actualAmount;
    
    @Column(name = "received_amount", precision = 10, scale = 2)
    private BigDecimal receivedAmount;
    
    @Column(name = "pending_amount", precision = 10, scale = 2)
    private BigDecimal pendingAmount;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Column(name = "status")
    private String status = "PENDING";
    
    @Column(name = "received_date")
    private LocalDateTime receivedDate;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    public BudgetRevenue(Budget budget, String revenueType, String description, BigDecimal estimatedAmount) {
        this.budget = budget;
        this.revenueType = revenueType;
        this.description = description;
        this.estimatedAmount = estimatedAmount;
    }
}
