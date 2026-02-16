package eventplanner.features.venue.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO for searching venues.
 * Geo-spatial search is now handled natively by PostGIS — radiusKm is
 * passed directly (no more degree-conversion hack).
 */
@Data
public class VenueSearchRequest {

    private String name;
    private String city;
    private String state;
    private String country;
    private Integer minCapacity;

    /** Centre-point latitude for radius search (PostGIS ST_DWithin). */
    private BigDecimal latitude;

    /** Centre-point longitude for radius search (PostGIS ST_DWithin). */
    private BigDecimal longitude;

    /** Search radius in kilometres (defaults to 10 km on the server side). */
    private BigDecimal radiusKm;

    // Bounding box search parameters (PostGIS ST_MakeEnvelope)
    private BigDecimal minLatitude;
    private BigDecimal maxLatitude;
    private BigDecimal minLongitude;
    private BigDecimal maxLongitude;

    private String venueType;
    private Boolean parkingRequired;
    private Boolean transitRequired;
}
