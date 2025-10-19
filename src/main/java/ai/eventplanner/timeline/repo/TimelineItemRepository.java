package ai.eventplanner.timeline.repo;

import ai.eventplanner.timeline.model.TimelineItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimelineItemRepository extends JpaRepository<TimelineItemEntity, UUID> {
    List<TimelineItemEntity> findByEventIdOrderByScheduledAtAsc(UUID eventId);
}


