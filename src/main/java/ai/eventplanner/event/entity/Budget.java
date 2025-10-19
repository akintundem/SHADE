package ai.eventplanner.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event budget management
 */
@Entity
@Table(name = "budgets")
public class Budget {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
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
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BudgetLineItem> lineItems;
    
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BudgetRevenue> revenues;
    
    // Constructors
    public Budget() {}
    
    public Budget(Event event, BigDecimal totalBudget) {
        this.event = event;
        this.totalBudget = totalBudget;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    
    public BigDecimal getTotalBudget() { return totalBudget; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public BigDecimal getContingencyPercentage() { return contingencyPercentage; }
    public void setContingencyPercentage(BigDecimal contingencyPercentage) { this.contingencyPercentage = contingencyPercentage; }
    
    public BigDecimal getContingencyAmount() { return contingencyAmount; }
    public void setContingencyAmount(BigDecimal contingencyAmount) { this.contingencyAmount = contingencyAmount; }
    
    public BigDecimal getTotalEstimated() { return totalEstimated; }
    public void setTotalEstimated(BigDecimal totalEstimated) { this.totalEstimated = totalEstimated; }
    
    public BigDecimal getTotalActual() { return totalActual; }
    public void setTotalActual(BigDecimal totalActual) { this.totalActual = totalActual; }
    
    public BigDecimal getVariance() { return variance; }
    public void setVariance(BigDecimal variance) { this.variance = variance; }
    
    public BigDecimal getVariancePercentage() { return variancePercentage; }
    public void setVariancePercentage(BigDecimal variancePercentage) { this.variancePercentage = variancePercentage; }
    
    public String getBudgetStatus() { return budgetStatus; }
    public void setBudgetStatus(String budgetStatus) { this.budgetStatus = budgetStatus; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    
    public LocalDateTime getApprovedDate() { return approvedDate; }
    public void setApprovedDate(LocalDateTime approvedDate) { this.approvedDate = approvedDate; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<BudgetLineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<BudgetLineItem> lineItems) { this.lineItems = lineItems; }
    
    public List<BudgetRevenue> getRevenues() { return revenues; }
    public void setRevenues(List<BudgetRevenue> revenues) { this.revenues = revenues; }
}
