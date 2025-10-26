package ai.eventplanner.checklist.repository;

import ai.eventplanner.checklist.entity.EventChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventChecklistRepository extends JpaRepository<EventChecklist, UUID> {
    
    List<EventChecklist> findByEventIdOrderByCreatedAtAsc(UUID eventId);
    
    List<EventChecklist> findByEventIdAndIsCompletedTrueOrderByCreatedAtAsc(UUID eventId);
    
    List<EventChecklist> findByEventIdAndIsCompletedFalseOrderByCreatedAtAsc(UUID eventId);
    
    List<EventChecklist> findByEventIdAndIsCompletedFalseAndDueDateBeforeOrderByDueDateAsc(UUID eventId, LocalDateTime date);
    
    List<EventChecklist> findByEventIdAndIsCompletedFalseAndDueDateBetweenOrderByDueDateAsc(UUID eventId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<EventChecklist> findByEventIdAndCategoryOrderByCreatedAtAsc(UUID eventId, String category);
    
    List<EventChecklist> findByEventIdAndIsCompletedFalse(UUID eventId);
    
    List<EventChecklist> findByEventIdAndIsCompletedTrue(UUID eventId);
    
    List<EventChecklist> findByEventId(UUID eventId);
    
    @Query("SELECT DISTINCT e.category FROM EventChecklist e WHERE e.eventId = :eventId AND e.category IS NOT NULL")
    List<String> findDistinctCategoriesByEventId(UUID eventId);
}
