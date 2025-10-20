package ai.eventplanner.vendor.entity;

import ai.eventplanner.event.entity.Event;
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
 * Event vendor relationships and contracts.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

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

    public enum VendorStatus {
        INQUIRY,
        RFP_SENT,
        QUOTE_RECEIVED,
        NEGOTIATION,
        CONFIRMED,
        CONTRACT_SIGNED,
        CANCELLED,
        COMPLETED
    }
}
