package eventplanner.features.venue.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Venue entity - normalized from events.venue JSON field.
 * Reusable venues across multiple events.
 */
@Entity
@Table(name = "venues",
    indexes = {
        @Index(name = "idx_venues_location", columnList = "city, state, country"),
        @Index(name = "idx_venues_coordinates", columnList = "latitude, longitude"),
        @Index(name = "idx_venues_name", columnList = "name")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@org.hibernate.annotations.SQLDelete(sql = "UPDATE venues SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
public class Venue extends BaseEntity {

    /**
     * Venue name
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Address line 1
     */
    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    /**
     * Address line 2 (optional)
     */
    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    /**
     * City
     */
    @Column(name = "city", length = 100)
    private String city;

    /**
     * State/Province
     */
    @Column(name = "state", length = 100)
    private String state;

    /**
     * Country
     */
    @Column(name = "country", length = 100)
    private String country;

    /**
     * Postal/ZIP code
     */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * Latitude for geo queries
     */
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    /**
     * Longitude for geo queries
     */
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    /**
     * Venue capacity (maximum attendees)
     */
    @Column(name = "capacity")
    private Integer capacity;

    /**
     * Venue type (CONFERENCE_CENTER, HOTEL, OUTDOOR, STADIUM, etc.)
     */
    @Column(name = "venue_type", length = 50)
    private String venueType;

    /**
     * Accessibility features description
     */
    @Column(name = "accessibility_features", columnDefinition = "TEXT")
    private String accessibilityFeatures;

    /**
     * Venue description/notes
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Contact phone number
     */
    @Column(name = "phone", length = 40)
    private String phone;

    /**
     * Contact email
     */
    @Column(name = "email", length = 180)
    private String email;

    /**
     * Website URL
     */
    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    /**
     * Parking availability
     */
    @Column(name = "parking_available")
    private Boolean parkingAvailable;

    /**
     * Public transit nearby
     */
    @Column(name = "public_transit_nearby")
    private Boolean publicTransitNearby;
}
