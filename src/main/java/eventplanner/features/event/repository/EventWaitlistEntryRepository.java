package eventplanner.features.event.repository;

import eventplanner.features.event.entity.EventWaitlistEntry;
import eventplanner.features.event.enums.EventWaitlistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventWaitlistEntryRepository extends JpaRepository<EventWaitlistEntry, UUID> {

    @Query("SELECT e FROM EventWaitlistEntry e WHERE e.id = :id AND e.event.id = :eventId")
    Optional<EventWaitlistEntry> findByIdAndEventId(@Param("id") UUID id, @Param("eventId") UUID eventId);

    @Query("SELECT e FROM EventWaitlistEntry e WHERE e.event.id = :eventId")
    Page<EventWaitlistEntry> findByEventId(@Param("eventId") UUID eventId, Pageable pageable);

    @Query("SELECT e FROM EventWaitlistEntry e WHERE e.event.id = :eventId AND e.status = :status")
    Page<EventWaitlistEntry> findByEventIdAndStatus(@Param("eventId") UUID eventId, @Param("status") EventWaitlistStatus status, Pageable pageable);

    @Query("SELECT e FROM EventWaitlistEntry e WHERE e.requester.id = :requesterId AND e.event.id = :eventId")
    List<EventWaitlistEntry> findByRequesterIdAndEventId(@Param("requesterId") UUID requesterId, @Param("eventId") UUID eventId);

    @Query("SELECT COUNT(e) > 0 FROM EventWaitlistEntry e WHERE e.event.id = :eventId AND e.requester.id = :requesterId AND e.status = :status")
    boolean existsByEventIdAndRequesterIdAndStatus(@Param("eventId") UUID eventId, @Param("requesterId") UUID requesterId, @Param("status") EventWaitlistStatus status);

    /**
     * Find waiting entries for an event, ordered by creation time (FIFO).
     */
    @Query("SELECT e FROM EventWaitlistEntry e WHERE e.event.id = :eventId AND e.status = :status ORDER BY e.createdAt ASC")
    List<EventWaitlistEntry> findByEventIdAndStatusOrderByCreatedAtAsc(
            @Param("eventId") UUID eventId, 
            @Param("status") EventWaitlistStatus status);

    /**
     * Count waiting entries for an event.
     */
    @Query("SELECT COUNT(e) FROM EventWaitlistEntry e WHERE e.event.id = :eventId AND e.status = :status")
    long countByEventIdAndStatus(@Param("eventId") UUID eventId, @Param("status") EventWaitlistStatus status);
}
