package ai.eventplanner.vendor.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Organization services and capabilities
 */
@Entity
@Table(name = "organization_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrganizationService extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    
    @Column(name = "service_name")
    private String serviceName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "subcategory")
    private String subcategory;
    
    @Column(name = "base_price", precision = 10, scale = 2)
    private BigDecimal basePrice;
    
    @Column(name = "price_unit")
    private String priceUnit; // per hour, per person, per event, etc.
    
    @Column(name = "min_duration_hours")
    private Integer minDurationHours;
    
    @Column(name = "max_duration_hours")
    private Integer maxDurationHours;
    
    @Column(name = "min_capacity")
    private Integer minCapacity;
    
    @Column(name = "max_capacity")
    private Integer maxCapacity;
    
    @Column(name = "is_available")
    private Boolean isAvailable = true;
    
    @Column(name = "setup_time_hours")
    private Integer setupTimeHours;
    
    @Column(name = "teardown_time_hours")
    private Integer teardownTimeHours;
    
    @Column(name = "equipment_included", columnDefinition = "TEXT")
    private String equipmentIncluded;
    
    @Column(name = "additional_services", columnDefinition = "TEXT")
    private String additionalServices;
    
    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;
    
    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;
    
    @Column(name = "deposit_percentage", precision = 5, scale = 2)
    private BigDecimal depositPercentage;
    
    @Column(name = "advance_booking_days")
    private Integer advanceBookingDays;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    public OrganizationService(Organization organization, String serviceName, String category) {
        this.organization = organization;
        this.serviceName = serviceName;
        this.category = category;
    }
}
