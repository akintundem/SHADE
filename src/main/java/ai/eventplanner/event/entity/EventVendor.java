package ai.eventplanner.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event vendor relationships and contracts
 */
@Entity
@Table(name = "event_vendors")
public class EventVendor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private ServiceProvider organization;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_status")
    private VendorStatus vendorStatus = VendorStatus.INQUIRY;
    
    @Column(name = "service_category")
    private String serviceCategory;
    
    @Column(name = "service_description", columnDefinition = "TEXT")
    private String serviceDescription;
    
    @Column(name = "quote_amount", precision = 10, scale = 2)
    private BigDecimal quoteAmount;
    
    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;
    
    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;
    
    @Column(name = "deposit_paid", precision = 10, scale = 2)
    private BigDecimal depositPaid;
    
    @Column(name = "balance_due", precision = 10, scale = 2)
    private BigDecimal balanceDue;
    
    @Column(name = "contract_url")
    private String contractUrl;
    
    @Column(name = "contract_signed")
    private Boolean contractSigned = false;
    
    @Column(name = "contract_signed_date")
    private LocalDateTime contractSignedDate;
    
    @Column(name = "rfp_sent_date")
    private LocalDateTime rfpSentDate;
    
    @Column(name = "quote_received_date")
    private LocalDateTime quoteReceivedDate;
    
    @Column(name = "booking_confirmed_date")
    private LocalDateTime bookingConfirmedDate;
    
    @Column(name = "service_date")
    private LocalDateTime serviceDate;
    
    @Column(name = "service_duration_hours")
    private Integer serviceDurationHours;
    
    @Column(name = "setup_time")
    private LocalDateTime setupTime;
    
    @Column(name = "teardown_time")
    private LocalDateTime teardownTime;
    
    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "rating")
    private Integer rating;
    
    @Column(name = "review", columnDefinition = "TEXT")
    private String review;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public EventVendor() {}
    
    public EventVendor(Event event, ServiceProvider organization, String serviceCategory) {
        this.event = event;
        this.organization = organization;
        this.serviceCategory = serviceCategory;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    
    public ServiceProvider getOrganization() { return organization; }
    public void setOrganization(ServiceProvider organization) { this.organization = organization; }
    
    public VendorStatus getVendorStatus() { return vendorStatus; }
    public void setVendorStatus(VendorStatus vendorStatus) { this.vendorStatus = vendorStatus; }
    
    public String getServiceCategory() { return serviceCategory; }
    public void setServiceCategory(String serviceCategory) { this.serviceCategory = serviceCategory; }
    
    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }
    
    public BigDecimal getQuoteAmount() { return quoteAmount; }
    public void setQuoteAmount(BigDecimal quoteAmount) { this.quoteAmount = quoteAmount; }
    
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    
    public BigDecimal getDepositPaid() { return depositPaid; }
    public void setDepositPaid(BigDecimal depositPaid) { this.depositPaid = depositPaid; }
    
    public BigDecimal getBalanceDue() { return balanceDue; }
    public void setBalanceDue(BigDecimal balanceDue) { this.balanceDue = balanceDue; }
    
    public String getContractUrl() { return contractUrl; }
    public void setContractUrl(String contractUrl) { this.contractUrl = contractUrl; }
    
    public Boolean getContractSigned() { return contractSigned; }
    public void setContractSigned(Boolean contractSigned) { this.contractSigned = contractSigned; }
    
    public LocalDateTime getContractSignedDate() { return contractSignedDate; }
    public void setContractSignedDate(LocalDateTime contractSignedDate) { this.contractSignedDate = contractSignedDate; }
    
    public LocalDateTime getRfpSentDate() { return rfpSentDate; }
    public void setRfpSentDate(LocalDateTime rfpSentDate) { this.rfpSentDate = rfpSentDate; }
    
    public LocalDateTime getQuoteReceivedDate() { return quoteReceivedDate; }
    public void setQuoteReceivedDate(LocalDateTime quoteReceivedDate) { this.quoteReceivedDate = quoteReceivedDate; }
    
    public LocalDateTime getBookingConfirmedDate() { return bookingConfirmedDate; }
    public void setBookingConfirmedDate(LocalDateTime bookingConfirmedDate) { this.bookingConfirmedDate = bookingConfirmedDate; }
    
    public LocalDateTime getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDateTime serviceDate) { this.serviceDate = serviceDate; }
    
    public Integer getServiceDurationHours() { return serviceDurationHours; }
    public void setServiceDurationHours(Integer serviceDurationHours) { this.serviceDurationHours = serviceDurationHours; }
    
    public LocalDateTime getSetupTime() { return setupTime; }
    public void setSetupTime(LocalDateTime setupTime) { this.setupTime = setupTime; }
    
    public LocalDateTime getTeardownTime() { return teardownTime; }
    public void setTeardownTime(LocalDateTime teardownTime) { this.teardownTime = teardownTime; }
    
    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public enum VendorStatus {
        INQUIRY,        // Initial inquiry sent
        RFP_SENT,       // Request for proposal sent
        QUOTED,         // Quote received
        NEGOTIATING,    // Negotiating terms
        BOOKED,         // Confirmed booking
        IN_PROGRESS,    // Service in progress
        COMPLETED,      // Service completed
        CANCELLED,      // Booking cancelled
        NO_RESPONSE     // No response from vendor
    }
}
