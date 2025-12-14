package eventplanner.features.event.repository;

import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventReminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventReminderRepository extends JpaRepository<EventReminder, UUID> {

    // UUID-based queries (for backward compatibility)
    List<EventReminder> findByEventIdOrderByReminderTimeAsc(UUID eventId);

    List<EventReminder> findByEventIdAndReminderTimeAfterOrderByReminderTimeAsc(UUID eventId, LocalDateTime after);

    Page<EventReminder> findByEventId(UUID eventId, Pageable pageable);
    
    // Relationship-based queries
    List<EventReminder> findByEventOrderByReminderTimeAsc(Event event);

    List<EventReminder> findByEventAndReminderTimeAfterOrderByReminderTimeAsc(Event event, LocalDateTime after);

    Page<EventReminder> findByEvent(Event event, Pageable pageable);

    /**
     * Find all active reminders that should be sent (reminder time has passed and not yet sent)
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT r FROM EventReminder r WHERE r.isActive = true " +
        "AND r.wasSent = false " +
        "AND r.reminderTime <= :now " +
        "ORDER BY r.reminderTime ASC"
    )
    List<EventReminder> findPendingRemindersToSend(@org.springframework.data.repository.query.Param("now") LocalDateTime now);
}


