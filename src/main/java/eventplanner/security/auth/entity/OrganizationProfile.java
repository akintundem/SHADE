package eventplanner.security.auth.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.domain.enums.OrganizationType;
import eventplanner.common.domain.enums.VendorProgramStatus;
import eventplanner.common.domain.enums.VendorTier;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "auth_organizations",
    indexes = {
        @Index(name = "idx_auth_org_name", columnList = "name"),
        @Index(name = "idx_auth_org_type", columnList = "type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class OrganizationProfile extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private OrganizationType type;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "phone_number", length = 40)
    private String phoneNumber;

    @Column(name = "contact_email", length = 180)
    private String contactEmail;

    @Column(name = "tax_id", length = 40)
    private String taxId;

    @Column(name = "registration_number", length = 60)
    private String registrationNumber;

    @Embedded
    private OrganizationAddress address;

    @Column(name = "google_place_id", length = 120, unique = true)
    private String googlePlaceId;

    @Column(name = "google_place_data", columnDefinition = "TEXT")
    private String googlePlaceData;

    @Column(name = "is_platform_vendor", nullable = false)
    @Builder.Default
    private Boolean isPlatformVendor = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_tier", length = 30)
    private VendorTier vendorTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_status", length = 30)
    private VendorProgramStatus vendorStatus;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "booking_count")
    @Builder.Default
    private Integer bookingCount = 0;
}
