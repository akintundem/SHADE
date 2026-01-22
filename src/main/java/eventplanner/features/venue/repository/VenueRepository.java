package eventplanner.features.venue.repository;

import eventplanner.features.venue.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Venue entity.
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
     * Find venues within a bounding box (for geo-based searches).
     * Simple implementation - for production, consider PostGIS or spatial indexes.
     */
    @Query("SELECT v FROM Venue v WHERE v.latitude BETWEEN :minLat AND :maxLat AND v.longitude BETWEEN :minLng AND :maxLng")
    List<Venue> findWithinBounds(
        @Param("minLat") BigDecimal minLat,
        @Param("maxLat") BigDecimal maxLat,
        @Param("minLng") BigDecimal minLng,
        @Param("maxLng") BigDecimal maxLng
    );
}
