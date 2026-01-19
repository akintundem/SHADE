package eventplanner.features.event.repository;

import eventplanner.features.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    Page<Event> findByEventStatus(String status, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.owner.id = :ownerId")
    Page<Event> findByOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdAt BETWEEN :startDate AND :endDate")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e.eventStatus, COUNT(e) FROM Event e WHERE e.createdAt BETWEEN :startDate AND :endDate GROUP BY e.eventStatus")
    Map<String, Long> getEventsByStatus(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT YEAR(e.createdAt) as year, MONTH(e.createdAt) as month, COUNT(e) as count FROM Event e WHERE e.createdAt BETWEEN :startDate AND :endDate GROUP BY YEAR(e.createdAt), MONTH(e.createdAt) ORDER BY YEAR(e.createdAt), MONTH(e.createdAt)")
    List<Map<String, Object>> getEventsByMonth(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    Long countByEventStatus(String status);
}
