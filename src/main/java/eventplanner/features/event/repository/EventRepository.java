package eventplanner.features.event.repository;

import eventplanner.features.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    /**
     * Atomically increment the current attendee count by the given delta.
     * Uses a single UPDATE statement to avoid read-modify-write race conditions.
     */
    @Modifying
    @Query("UPDATE Event e SET e.currentAttendeeCount = COALESCE(e.currentAttendeeCount, 0) + :delta WHERE e.id = :eventId")
    int incrementAttendeeCount(@Param("eventId") UUID eventId, @Param("delta") int delta);

    /**
     * Atomically decrement the current attendee count, flooring at 0.
     * Uses GREATEST to prevent negative counts.
     */
    @Modifying
    @Query(value = "UPDATE events SET current_attendee_count = GREATEST(0, COALESCE(current_attendee_count, 0) + :delta) WHERE id = :eventId", nativeQuery = true)
    int decrementAttendeeCount(@Param("eventId") UUID eventId, @Param("delta") int delta);

    /**
     * Fetch event with pessimistic write lock for capacity-sensitive operations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") UUID id);

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

    /**
     * Find public events owned by any of the specified users (for following feed)
     */
    @Query("SELECT e FROM Event e WHERE e.owner.id IN :ownerIds AND e.isPublic = true ORDER BY e.startDateTime DESC")
    Page<Event> findByOwnerIdInAndIsPublicTrue(@Param("ownerIds") List<UUID> ownerIds, Pageable pageable);
}
