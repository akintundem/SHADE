package eventplanner.features.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Embeddable Venue entity containing location and venue information.
 * This can be embedded directly into the Event entity.
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

    @Column(name = "venue_google_place_id", length = 255)
    private String googlePlaceId;

    @Column(name = "venue_google_place_data", columnDefinition = "TEXT")
    private String googlePlaceData;
}

