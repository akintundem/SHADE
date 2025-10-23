package ai.eventplanner.vendor.entity;

import ai.eventplanner.event.entity.Event;
import ai.eventplanner.common.domain.enums.ContractStatus;
import ai.eventplanner.common.domain.enums.QuoteStatus;
import ai.eventplanner.common.domain.enums.OrganizationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event vendor with complete organization information and planning coordination.
 */
@Entity
@Table(name = "event_vendors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // Organization Information
    @Column(name = "vendor_name", nullable = false)
    private String vendorName;
    
    @Column(name = "legal_name")
    private String legalName;
    
    @Column(name = "vendor_email")
    private String vendorEmail;
    
    @Column(name = "vendor_phone")
    private String vendorPhone;
    
    @Column(name = "website_url")
    private String websiteUrl;
    
    @Column(name = "logo_url")
    private String logoUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type")
    private OrganizationType organizationType;
    
    @Column(name = "vendor_description", columnDefinition = "TEXT")
    private String vendorDescription;
    
    @Column(name = "vendor_address", columnDefinition = "TEXT")
    private String vendorAddress;
    
    @Column(name = "vendor_city")
    private String vendorCity;
    
    @Column(name = "vendor_state")
    private String vendorState;
    
    @Column(name = "vendor_country")
    private String vendorCountry;
    
    @Column(name = "vendor_postal_code")
    private String vendorPostalCode;
    
    @Column(name = "vendor_latitude")
    private Double vendorLatitude;
    
    @Column(name = "vendor_longitude")
    private Double vendorLongitude;
    
    @Column(name = "google_place_id")
    private String googlePlaceId;
    
    @Column(name = "business_license")
    private String businessLicense;
    
    @Column(name = "tax_id")
    private String taxId;
    
    @Column(name = "insurance_info", columnDefinition = "TEXT")
    private String insuranceInfo;
    
    @Column(name = "certifications", columnDefinition = "TEXT")
    private String certifications;
    
    @Column(name = "vendor_rating")
    private Double vendorRating;
    
    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;
    
    @Column(name = "price_tier")
    private String priceTier; // $, $$, $$$, $$$$
    
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Vendor Status and Planning
    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_status")
    @Builder.Default
    private VendorStatus vendorStatus = VendorStatus.INQUIRY;

    @Column(name = "service_category")
    private String serviceCategory;

    @Column(name = "service_description", columnDefinition = "TEXT")
    private String serviceDescription;

    @Column(name = "quote_amount", precision = 10, scale = 2)
    private BigDecimal quoteAmount;

    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "estimated_deposit", precision = 10, scale = 2)
    private BigDecimal estimatedDeposit;

    @Column(name = "contract_amount", precision = 10, scale = 2)
    private BigDecimal contractAmount;

    @Column(name = "total_estimated_cost", precision = 10, scale = 2)
    private BigDecimal totalEstimatedCost;

    @Column(name = "contract_url")
    private String contractUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_status")
    @Builder.Default
    private ContractStatus contractStatus = ContractStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_status")
    @Builder.Default
    private QuoteStatus quoteStatus = QuoteStatus.REQUESTED;

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
    
    // Constructor for creating a new vendor
    public EventVendor(Event event, String vendorName, OrganizationType organizationType, String serviceCategory) {
        this.event = event;
        this.vendorName = vendorName;
        this.organizationType = organizationType;
        this.serviceCategory = serviceCategory;
        this.vendorStatus = VendorStatus.INQUIRY;
    }
}
