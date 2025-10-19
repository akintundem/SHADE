package ai.eventplanner.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Organization services and capabilities
 */
@Entity
@Table(name = "organization_services")
public class OrganizationService {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private ServiceProvider organization;
    
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
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public OrganizationService() {}
    
    public OrganizationService(ServiceProvider organization, String serviceName, String category) {
        this.organization = organization;
        this.serviceName = serviceName;
        this.category = category;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public ServiceProvider getOrganization() { return organization; }
    public void setOrganization(ServiceProvider organization) { this.organization = organization; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }
    
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    
    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    
    public Integer getMinDurationHours() { return minDurationHours; }
    public void setMinDurationHours(Integer minDurationHours) { this.minDurationHours = minDurationHours; }
    
    public Integer getMaxDurationHours() { return maxDurationHours; }
    public void setMaxDurationHours(Integer maxDurationHours) { this.maxDurationHours = maxDurationHours; }
    
    public Integer getMinCapacity() { return minCapacity; }
    public void setMinCapacity(Integer minCapacity) { this.minCapacity = minCapacity; }
    
    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
    
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
    
    public Integer getSetupTimeHours() { return setupTimeHours; }
    public void setSetupTimeHours(Integer setupTimeHours) { this.setupTimeHours = setupTimeHours; }
    
    public Integer getTeardownTimeHours() { return teardownTimeHours; }
    public void setTeardownTimeHours(Integer teardownTimeHours) { this.teardownTimeHours = teardownTimeHours; }
    
    public String getEquipmentIncluded() { return equipmentIncluded; }
    public void setEquipmentIncluded(String equipmentIncluded) { this.equipmentIncluded = equipmentIncluded; }
    
    public String getAdditionalServices() { return additionalServices; }
    public void setAdditionalServices(String additionalServices) { this.additionalServices = additionalServices; }
    
    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }
    
    public String getCancellationPolicy() { return cancellationPolicy; }
    public void setCancellationPolicy(String cancellationPolicy) { this.cancellationPolicy = cancellationPolicy; }
    
    public BigDecimal getDepositPercentage() { return depositPercentage; }
    public void setDepositPercentage(BigDecimal depositPercentage) { this.depositPercentage = depositPercentage; }
    
    public Integer getAdvanceBookingDays() { return advanceBookingDays; }
    public void setAdvanceBookingDays(Integer advanceBookingDays) { this.advanceBookingDays = advanceBookingDays; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
