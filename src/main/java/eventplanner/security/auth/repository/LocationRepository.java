package eventplanner.security.auth.repository;

import eventplanner.security.auth.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    @Query("SELECT l FROM Location l WHERE " +
           "LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "(l.state IS NOT NULL AND LOWER(l.state) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "LOWER(l.country) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Location> searchByQuery(@Param("query") String query);
}

