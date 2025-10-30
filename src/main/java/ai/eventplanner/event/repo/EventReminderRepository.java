package ai.eventplanner.event.repo;

import ai.eventplanner.event.entity.EventReminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventReminderRepository extends JpaRepository<EventReminder, UUID> {

    List<EventReminder> findByEventIdOrderByReminderTimeAsc(UUID eventId);

    List<EventReminder> findByEventIdAndReminderTimeAfterOrderByReminderTimeAsc(UUID eventId, LocalDateTime after);

    Page<EventReminder> findByEventId(UUID eventId, Pageable pageable);
}


