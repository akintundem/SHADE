package eventplanner.features.timeline.repository;

import eventplanner.common.domain.enums.TimelineStatus;
import eventplanner.features.timeline.entity.TimelineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for timeline items with advanced query methods
 */
@Repository
public interface TimelineItemRepository extends JpaRepository<TimelineItem, UUID> {
    
    // Basic queries
    List<TimelineItem> findByEventIdOrderByScheduledAtAsc(UUID eventId);
    
    List<TimelineItem> findByEventId(UUID eventId);
    
    Optional<TimelineItem> findByIdAndEventId(UUID id, UUID eventId);
    
    // Hierarchy queries
    List<TimelineItem> findByEventIdAndParentTaskIdIsNullOrderByTaskOrderAsc(UUID eventId);
    
    List<TimelineItem> findByParentTaskIdOrderByTaskOrderAsc(UUID parentTaskId);
    
    List<TimelineItem> findByEventIdAndIsParentTaskTrue(UUID eventId);
    
    // Status queries
    List<TimelineItem> findByEventIdAndStatus(UUID eventId, TimelineStatus status);
    
    long countByEventIdAndStatus(UUID eventId, TimelineStatus status);
    
    // Assignee queries
    List<TimelineItem> findByEventIdAndAssignedTo(UUID eventId, UUID assignedTo);
    
    // Date range queries
    @Query("SELECT t FROM TimelineItem t WHERE t.eventId = :eventId " +
           "AND t.startDate IS NOT NULL " +
           "AND t.startDate >= :startDate AND t.startDate <= :endDate " +
           "ORDER BY t.startDate ASC")
    List<TimelineItem> findByEventIdAndDateRange(
        @Param("eventId") UUID eventId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT t FROM TimelineItem t WHERE t.eventId = :eventId " +
           "AND t.dueDate IS NOT NULL " +
           "AND t.dueDate < :currentDate " +
           "AND t.status NOT IN (eventplanner.common.domain.enums.TimelineStatus.COMPLETED, " +
           "eventplanner.common.domain.enums.TimelineStatus.DONE, " +
           "eventplanner.common.domain.enums.TimelineStatus.CANCELLED) " +
           "ORDER BY t.dueDate ASC")
    List<TimelineItem> findOverdueTasks(
        @Param("eventId") UUID eventId,
        @Param("currentDate") LocalDateTime currentDate
    );
    
    @Query("SELECT t FROM TimelineItem t WHERE t.eventId = :eventId " +
           "AND t.dueDate IS NOT NULL " +
           "AND t.dueDate >= :currentDate " +
           "AND t.dueDate <= :futureDate " +
           "AND t.status NOT IN (eventplanner.common.domain.enums.TimelineStatus.COMPLETED, " +
           "eventplanner.common.domain.enums.TimelineStatus.DONE) " +
           "ORDER BY t.dueDate ASC")
    List<TimelineItem> findUpcomingTasks(
        @Param("eventId") UUID eventId,
        @Param("currentDate") LocalDateTime currentDate,
        @Param("futureDate") LocalDateTime futureDate
    );
    
    // Category and priority queries
    List<TimelineItem> findByEventIdAndCategory(UUID eventId, String category);
    
    List<TimelineItem> findByEventIdAndPriority(UUID eventId, String priority);
    
    // Search queries
    @Query("SELECT t FROM TimelineItem t WHERE t.eventId = :eventId " +
           "AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY t.startDate ASC")
    List<TimelineItem> searchByEventIdAndQuery(
        @Param("eventId") UUID eventId,
        @Param("query") String query
    );
    
    // Progress and statistics queries
    @Query("SELECT COUNT(t) FROM TimelineItem t WHERE t.eventId = :eventId")
    long countByEventId(@Param("eventId") UUID eventId);
    
    @Query("SELECT COALESCE(AVG(t.progressPercentage), 0) FROM TimelineItem t " +
           "WHERE t.eventId = :eventId AND t.isParentTask = true")
    Double getAverageProgressForEvent(@Param("eventId") UUID eventId);
    
    @Query("SELECT MIN(t.startDate) FROM TimelineItem t WHERE t.eventId = :eventId AND t.startDate IS NOT NULL")
    LocalDateTime getEarliestDate(@Param("eventId") UUID eventId);
    
    @Query("SELECT MAX(COALESCE(t.endTime, t.dueDate, t.startDate)) FROM TimelineItem t " +
           "WHERE t.eventId = :eventId AND (t.endTime IS NOT NULL OR t.dueDate IS NOT NULL OR t.startDate IS NOT NULL)")
    LocalDateTime getLatestDate(@Param("eventId") UUID eventId);
    
    // Note: Dependencies checking handled in service layer due to UUID array limitations
}


