package eventplanner.features.venue.repository;

import eventplanner.features.venue.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Venue entity.
 * Geo-spatial queries powered by PostGIS (SRID 4326, geography casts for metre accuracy).
 */
@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {

    /**
     * Find venues by city.
     */
    @Query("SELECT v FROM Venue v WHERE LOWER(v.city) = LOWER(:city)")
    List<Venue> findByCity(@Param("city") String city);

    /**
     * Find venues by city and state.
     */
    @Query("SELECT v FROM Venue v WHERE LOWER(v.city) = LOWER(:city) AND LOWER(v.state) = LOWER(:state)")
    List<Venue> findByCityAndState(@Param("city") String city, @Param("state") String state);

    /**
     * Find venues by country.
     */
    @Query("SELECT v FROM Venue v WHERE LOWER(v.country) = LOWER(:country)")
    List<Venue> findByCountry(@Param("country") String country);

    /**
     * Search venues by name (partial match).
     */
    @Query("SELECT v FROM Venue v WHERE LOWER(v.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Venue> searchByName(@Param("name") String name);

    /**
     * PostGIS: find venues within {@code radiusMeters} of a point, ordered by distance.
     * Supports optional server-side filters for venueType, parking, and transit.
     */
    @Query(value =
        "SELECT * FROM venues v " +
        "WHERE v.deleted_at IS NULL " +
        "  AND v.location IS NOT NULL " +
        "  AND ST_DWithin(" +
        "        v.location::geography, " +
        "        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, " +
        "        :radiusMeters" +
        "      ) " +
        "  AND (:venueType IS NULL OR LOWER(v.venue_type) = LOWER(:venueType)) " +
        "  AND (:parkingRequired = false OR v.parking_available = true) " +
        "  AND (:transitRequired = false OR v.public_transit_nearby = true) " +
        "ORDER BY ST_Distance(" +
        "        v.location::geography, " +
        "        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography" +
        "      )",
        nativeQuery = true)
    List<Venue> findNearLocation(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radiusMeters") double radiusMeters,
        @Param("venueType") String venueType,
        @Param("parkingRequired") boolean parkingRequired,
        @Param("transitRequired") boolean transitRequired
    );

    /**
     * PostGIS: find venues inside a bounding box.
     * Supports optional server-side filters for venueType, parking, and transit.
     */
    @Query(value =
        "SELECT * FROM venues v " +
        "WHERE v.deleted_at IS NULL " +
        "  AND v.location IS NOT NULL " +
        "  AND ST_Within(v.location, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326))" +
        "  AND (:venueType IS NULL OR LOWER(v.venue_type) = LOWER(:venueType)) " +
        "  AND (:parkingRequired = false OR v.parking_available = true) " +
        "  AND (:transitRequired = false OR v.public_transit_nearby = true) ",
        nativeQuery = true)
    List<Venue> findWithinBounds(
        @Param("minLat") double minLat,
        @Param("maxLat") double maxLat,
        @Param("minLng") double minLng,
        @Param("maxLng") double maxLng,
        @Param("venueType") String venueType,
        @Param("parkingRequired") boolean parkingRequired,
        @Param("transitRequired") boolean transitRequired
    );

    /**
     * PostGIS: calculate distance in metres between a venue and a point.
     */
    @Query(value =
        "SELECT ST_Distance(v.location::geography, " +
        "       ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) " +
        "FROM venues v WHERE v.id = :venueId",
        nativeQuery = true)
    Double calculateDistanceMeters(
        @Param("venueId") UUID venueId,
        @Param("lat") double lat,
        @Param("lng") double lng
    );
}
