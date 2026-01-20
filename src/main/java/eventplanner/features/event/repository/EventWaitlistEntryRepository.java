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

    Optional<EventWaitlistEntry> findByIdAndEventId(UUID id, UUID eventId);

    Page<EventWaitlistEntry> findByEventId(UUID eventId, Pageable pageable);

    Page<EventWaitlistEntry> findByEventIdAndStatus(UUID eventId, EventWaitlistStatus status, Pageable pageable);

    List<EventWaitlistEntry> findByRequesterIdAndEventId(UUID requesterId, UUID eventId);

    boolean existsByEventIdAndRequesterIdAndStatus(UUID eventId, UUID requesterId, EventWaitlistStatus status);

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
    long countByEventIdAndStatus(UUID eventId, EventWaitlistStatus status);
}
