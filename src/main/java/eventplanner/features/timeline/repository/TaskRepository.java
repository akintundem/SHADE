package eventplanner.features.timeline.repository;

import eventplanner.features.timeline.entity.Task;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    @Query("SELECT t FROM Task t WHERE t.event.id = :eventId ORDER BY t.taskOrder ASC")
    List<Task> findByEventIdOrderByTaskOrderAsc(@Param("eventId") UUID eventId);
    
    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.event.id = :eventId")
    Optional<Task> findByIdAndEventId(@Param("id") UUID id, @Param("eventId") UUID eventId);

    /**
     * Acquire pessimistic write lock on task for safe progress recalculation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findByIdForUpdate(@Param("id") UUID id);
}

