package ai.eventplanner.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ServiceProviders are service providers (vendors, venues, caterers, etc.)
 * This is distinct from Users who are event creators/attendees
 */
@Entity
@Table(name = "service_providers")
public class ServiceProvider {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
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
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventVendor> eventVendors;
    
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrganizationService> services;
    
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrganizationReview> reviews;
    
    // Constructors
    public ServiceProvider() {}
    
    public ServiceProvider(String name, OrganizationType organizationType) {
        this.name = name;
        this.organizationType = organizationType;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    
    public OrganizationType getOrganizationType() { return organizationType; }
    public void setOrganizationType(OrganizationType organizationType) { this.organizationType = organizationType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public String getGooglePlaceId() { return googlePlaceId; }
    public void setGooglePlaceId(String googlePlaceId) { this.googlePlaceId = googlePlaceId; }
    
    public String getBusinessLicense() { return businessLicense; }
    public void setBusinessLicense(String businessLicense) { this.businessLicense = businessLicense; }
    
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    
    public String getInsuranceInfo() { return insuranceInfo; }
    public void setInsuranceInfo(String insuranceInfo) { this.insuranceInfo = insuranceInfo; }
    
    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }
    
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    
    public String getPriceTier() { return priceTier; }
    public void setPriceTier(String priceTier) { this.priceTier = priceTier; }
    
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<EventVendor> getEventVendors() { return eventVendors; }
    public void setEventVendors(List<EventVendor> eventVendors) { this.eventVendors = eventVendors; }
    
    public List<OrganizationService> getServices() { return services; }
    public void setServices(List<OrganizationService> services) { this.services = services; }
    
    public List<OrganizationReview> getReviews() { return reviews; }
    public void setReviews(List<OrganizationReview> reviews) { this.reviews = reviews; }
    
    public enum OrganizationType {
        VENUE,          // Event venues, halls, outdoor spaces
        CATERING,       // Food and beverage services
        ENTERTAINMENT,  // DJs, bands, performers, MCs
        PHOTOGRAPHY,    // Photographers, videographers
        DECORATION,     // Floral, decor, signage
        AV_TECH,        // Audio-visual, lighting, staging
        SECURITY,       // Security services
        TRANSPORTATION, // Transportation, valet
        STAFFING,       // Event staff, volunteers
        MARKETING,      // Marketing agencies, PR
        OTHER           // Other service providers
    }
}
