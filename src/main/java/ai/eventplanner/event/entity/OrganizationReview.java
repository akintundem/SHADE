package ai.eventplanner.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Organization reviews and ratings
 */
@Entity
@Table(name = "organization_reviews")
public class OrganizationReview {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private ServiceProvider organization;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private EventUser user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;
    
    @Column(name = "rating")
    private Integer rating; // 1-5 stars
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;
    
    @Column(name = "service_category")
    private String serviceCategory;
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Column(name = "is_public")
    private Boolean isPublic = true;
    
    @Column(name = "helpful_count")
    private Integer helpfulCount = 0;
    
    @Column(name = "not_helpful_count")
    private Integer notHelpfulCount = 0;
    
    @Column(name = "response_text", columnDefinition = "TEXT")
    private String responseText;
    
    @Column(name = "response_date")
    private LocalDateTime responseDate;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public OrganizationReview() {}
    
    public OrganizationReview(ServiceProvider organization, EventUser user, Integer rating, String reviewText) {
        this.organization = organization;
        this.user = user;
        this.rating = rating;
        this.reviewText = reviewText;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public ServiceProvider getOrganization() { return organization; }
    public void setOrganization(ServiceProvider organization) { this.organization = organization; }
    
    public EventUser getUser() { return user; }
    public void setUser(EventUser user) { this.user = user; }
    
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    
    public String getServiceCategory() { return serviceCategory; }
    public void setServiceCategory(String serviceCategory) { this.serviceCategory = serviceCategory; }
    
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public Integer getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(Integer helpfulCount) { this.helpfulCount = helpfulCount; }
    
    public Integer getNotHelpfulCount() { return notHelpfulCount; }
    public void setNotHelpfulCount(Integer notHelpfulCount) { this.notHelpfulCount = notHelpfulCount; }
    
    public String getResponseText() { return responseText; }
    public void setResponseText(String responseText) { this.responseText = responseText; }
    
    public LocalDateTime getResponseDate() { return responseDate; }
    public void setResponseDate(LocalDateTime responseDate) { this.responseDate = responseDate; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
