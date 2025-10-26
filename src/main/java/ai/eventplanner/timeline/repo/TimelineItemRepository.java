package ai.eventplanner.timeline.repo;

import ai.eventplanner.timeline.model.TimelineItemEntity;
import ai.eventplanner.common.domain.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TimelineItemRepository extends JpaRepository<TimelineItemEntity, UUID> {
    List<TimelineItemEntity> findByEventIdOrderByScheduledAtAsc(UUID eventId);
    
    List<TimelineItemEntity> findByEventIdAndScheduledAtBetweenOrderByScheduledAtAsc(UUID eventId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<TimelineItemEntity> findByEventIdAndScheduledAtBeforeAndStatusNotOrderByScheduledAtAsc(UUID eventId, LocalDateTime date, Status status);
    
    @Query("SELECT t FROM TimelineItemEntity t WHERE :dependencyId MEMBER OF t.dependencies")
    List<TimelineItemEntity> findByDependenciesContaining(@Param("dependencyId") UUID dependencyId);
}


