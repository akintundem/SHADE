package eventplanner.features.venue.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO for searching venues
 */
@Data
public class VenueSearchRequest {

    private String name;
    private String city;
    private String state;
    private String country;
    private Integer minCapacity;

    // Geo-spatial search parameters
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal radiusKm;

    // Bounding box search parameters
    private BigDecimal minLatitude;
    private BigDecimal maxLatitude;
    private BigDecimal minLongitude;
    private BigDecimal maxLongitude;

    private String venueType;
    private Boolean parkingRequired;
    private Boolean transitRequired;

    /**
     * Convert radius in kilometers to degrees (approximate)
     * ~111km per degree latitude
     */
    public BigDecimal getRadiusDegrees() {
        if (radiusKm == null) {
            return BigDecimal.valueOf(0.09); // Default ~10km
        }
        return radiusKm.divide(BigDecimal.valueOf(111), 6, java.math.RoundingMode.HALF_UP);
    }
}
