package ai.eventplanner.risk.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import ai.eventplanner.common.domain.enums.RiskType;
import ai.eventplanner.common.domain.enums.Severity;
import ai.eventplanner.common.domain.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event risk management and monitoring
 */
@Entity
@Table(name = "event_risks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventRisk extends BaseEntity {
    
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    
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
    private Status status = Status.PENDING;
    
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
    
    public EventRisk(UUID eventId, RiskType riskType, String title, String description) {
        this.eventId = eventId;
        this.riskType = riskType;
        this.title = title;
        this.description = description;
        this.detectedAt = LocalDateTime.now();
    }
}
