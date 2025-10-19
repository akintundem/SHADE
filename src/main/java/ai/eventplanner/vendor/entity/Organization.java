package ai.eventplanner.vendor.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import ai.eventplanner.common.domain.enums.OrganizationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Organizations are service providers (vendors, venues, caterers, etc.)
 * This is distinct from Users who are event creators/attendees
 */
@Entity
@Table(name = "organizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Organization extends BaseEntity {
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "legal_name")
    private String legalName;
    
    @Column(unique = true)
    private String email;
    
    private String phone;
    
    @Column(name = "website_url")
    private String websiteUrl;
    
    @Column(name = "logo_url")
    private String logoUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type")
    private OrganizationType organizationType;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "country")
    private String country;
    
    @Column(name = "postal_code")
    private String postalCode;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
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
    
    @Column(name = "rating")
    private Double rating;
    
    @Column(name = "review_count")
    private Integer reviewCount = 0;
    
    @Column(name = "price_tier")
    private String priceTier; // $, $$, $$$, $$$$
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // Relationships
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrganizationService> services;
    
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrganizationReview> reviews;
    
    public Organization(String name, OrganizationType organizationType) {
        this.name = name;
        this.organizationType = organizationType;
    }
}
