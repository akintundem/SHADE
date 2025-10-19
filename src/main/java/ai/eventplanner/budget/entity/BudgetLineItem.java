package ai.eventplanner.budget.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Budget line items for detailed expense tracking
 */
@Entity
@Table(name = "budget_line_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BudgetLineItem extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    @Column(name = "category")
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
    private Integer quantity = 1;
    
    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;
    
    @Column(name = "payment_status")
    private String paymentStatus = "PENDING";
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
    
    @Column(name = "invoice_number")
    private String invoiceNumber;
    
    @Column(name = "invoice_url")
    private String invoiceUrl;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "is_essential")
    private Boolean isEssential = false;
    
    @Column(name = "priority")
    private String priority = "MEDIUM";
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    public BudgetLineItem(Budget budget, String category, String description, BigDecimal estimatedCost) {
        this.budget = budget;
        this.category = category;
        this.description = description;
        this.estimatedCost = estimatedCost;
    }
}
