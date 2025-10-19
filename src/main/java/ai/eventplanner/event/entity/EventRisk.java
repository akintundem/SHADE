package ai.eventplanner.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event risk management and monitoring
 */
@Entity
@Table(name = "event_risks")
public class EventRisk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_type")
    private RiskType riskType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity;
    
    @Column(name = "probability", precision = 3, scale = 2)
    private BigDecimal probability; // 0.0 to 1.0
    
    @Column(name = "impact_score")
    private Integer impactScore; // 1-10
    
    @Column(name = "risk_score")
    private Integer riskScore; // Calculated: probability * impact
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "mitigation_plan", columnDefinition = "TEXT")
    private String mitigationPlan;
    
    @Column(name = "contingency_plan", columnDefinition = "TEXT")
    private String contingencyPlan;
    
    @Column(name = "assigned_to")
    private String assignedTo;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.IDENTIFIED;
    
    @Column(name = "detected_at")
    private LocalDateTime detectedAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "review_date")
    private LocalDateTime reviewDate;
    
    @Column(name = "cost_impact", precision = 10, scale = 2)
    private BigDecimal costImpact;
    
    @Column(name = "schedule_impact_hours")
    private Integer scheduleImpactHours;
    
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
    public EventRisk() {}
    
    public EventRisk(Event event, RiskType riskType, String title, String description) {
        this.event = event;
        this.riskType = riskType;
        this.title = title;
        this.description = description;
        this.detectedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    
    public RiskType getRiskType() { return riskType; }
    public void setRiskType(RiskType riskType) { this.riskType = riskType; }
    
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    
    public BigDecimal getProbability() { return probability; }
    public void setProbability(BigDecimal probability) { this.probability = probability; }
    
    public Integer getImpactScore() { return impactScore; }
    public void setImpactScore(Integer impactScore) { this.impactScore = impactScore; }
    
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getMitigationPlan() { return mitigationPlan; }
    public void setMitigationPlan(String mitigationPlan) { this.mitigationPlan = mitigationPlan; }
    
    public String getContingencyPlan() { return contingencyPlan; }
    public void setContingencyPlan(String contingencyPlan) { this.contingencyPlan = contingencyPlan; }
    
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
    
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    
    public LocalDateTime getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate) { this.reviewDate = reviewDate; }
    
    public BigDecimal getCostImpact() { return costImpact; }
    public void setCostImpact(BigDecimal costImpact) { this.costImpact = costImpact; }
    
    public Integer getScheduleImpactHours() { return scheduleImpactHours; }
    public void setScheduleImpactHours(Integer scheduleImpactHours) { this.scheduleImpactHours = scheduleImpactHours; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public enum RiskType {
        WEATHER,
        VENDOR_NOSHOW,
        BUDGET_OVERRUN,
        ATTENDANCE_LOW,
        TECHNICAL_FAILURE,
        SECURITY_INCIDENT,
        HEALTH_EMERGENCY,
        VENUE_ISSUE,
        TRANSPORTATION_DELAY,
        SUPPLY_SHORTAGE,
        REGULATORY_COMPLIANCE,
        MARKETING_FAILURE,
        OTHER
    }
    
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    public enum Status {
        IDENTIFIED,
        ASSESSED,
        MITIGATED,
        MONITORING,
        RESOLVED,
        OCCURRED
    }
}
