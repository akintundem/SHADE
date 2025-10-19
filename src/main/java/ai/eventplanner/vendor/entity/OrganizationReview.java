package ai.eventplanner.vendor.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Organization reviews and ratings
 */
@Entity
@Table(name = "organization_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrganizationReview extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "event_id")
    private UUID eventId;
    
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
    private java.time.LocalDateTime responseDate;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    public OrganizationReview(Organization organization, UUID userId, Integer rating, String reviewText) {
        this.organization = organization;
        this.userId = userId;
        this.rating = rating;
        this.reviewText = reviewText;
    }
}
