package eventplanner.features.event.entity;

import eventplanner.common.util.GeoUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;

/**
 * Embeddable Venue entity containing location and venue information.
 * This can be embedded directly into the Event entity.
 * Uses PostGIS geometry(Point, 4326) for spatial queries.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venue {

    @Column(name = "venue_address", length = 255)
    private String address;

    @Column(name = "venue_city", length = 100)
    private String city;

    @Column(name = "venue_state", length = 100)
    private String state;

    @Column(name = "venue_country", length = 100)
    private String country;

    @Column(name = "venue_zip_code", length = 20)
    private String zipCode;

    @Column(name = "venue_latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "venue_longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    /**
     * PostGIS Point (SRID 4326) for spatial queries on events.
     */
    @Column(name = "venue_location", columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "venue_google_place_id", length = 255)
    private String googlePlaceId;

    @Column(name = "venue_google_place_data", columnDefinition = "TEXT")
    private String googlePlaceData;

    /**
     * Sync the PostGIS Point from latitude/longitude.
     * Called by Event entity's @PrePersist / @PreUpdate.
     */
    public void syncLocation() {
        this.location = GeoUtils.createPoint(latitude, longitude);
    }
}

