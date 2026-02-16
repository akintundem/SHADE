package eventplanner.security.auth.repository;

import eventplanner.security.auth.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Location entity.
 * PostGIS spatial queries for nearest-location lookups.
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    @Query("SELECT l FROM Location l WHERE " +
           "LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "(l.state IS NOT NULL AND LOWER(l.state) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "LOWER(l.country) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Location> searchByQuery(@Param("query") String query);

    Optional<Location> findFirstByCityIgnoreCaseAndStateIgnoreCaseAndCountryIgnoreCase(String city, String state, String country);

    Optional<Location> findFirstByCityIgnoreCaseAndCountryIgnoreCase(String city, String country);

    /**
     * PostGIS: find the single nearest location to a coordinate.
     * Useful for tax-rate resolution by proximity when city name matching fails.
     */
    @Query(value =
        "SELECT * FROM locations l " +
        "WHERE l.location IS NOT NULL " +
        "ORDER BY ST_Distance(l.location::geography, " +
        "         ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) " +
        "LIMIT 1",
        nativeQuery = true)
    Optional<Location> findNearestLocation(@Param("lat") double lat, @Param("lng") double lng);

    /**
     * PostGIS: find locations within a radius (metres), ordered by distance.
     */
    @Query(value =
        "SELECT * FROM locations l " +
        "WHERE l.location IS NOT NULL " +
        "  AND ST_DWithin(" +
        "        l.location::geography, " +
        "        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, " +
        "        :radiusMeters" +
        "      ) " +
        "ORDER BY ST_Distance(l.location::geography, " +
        "         ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)",
        nativeQuery = true)
    List<Location> findLocationsNearPoint(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radiusMeters") double radiusMeters
    );
}
