package ai.eventplanner.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Budget revenue sources (ticket sales, sponsorships, etc.)
 */
@Entity
@Table(name = "budget_revenues")
public class BudgetRevenue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
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
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public BudgetRevenue() {}
    
    public BudgetRevenue(Budget budget, String revenueType, String description, BigDecimal estimatedAmount) {
        this.budget = budget;
        this.revenueType = revenueType;
        this.description = description;
        this.estimatedAmount = estimatedAmount;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }
    
    public String getRevenueType() { return revenueType; }
    public void setRevenueType(String revenueType) { this.revenueType = revenueType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getEstimatedAmount() { return estimatedAmount; }
    public void setEstimatedAmount(BigDecimal estimatedAmount) { this.estimatedAmount = estimatedAmount; }
    
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
    
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDateTime receivedDate) { this.receivedDate = receivedDate; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
