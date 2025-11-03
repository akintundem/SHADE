package ai.eventplanner.timeline.repo;

import ai.eventplanner.common.domain.enums.TimelineStatus;
import ai.eventplanner.timeline.model.TimelineItemEntity;
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
public interface TimelineItemRepository extends JpaRepository<TimelineItemEntity, UUID> {
    
    // Basic queries
    List<TimelineItemEntity> findByEventIdOrderByScheduledAtAsc(UUID eventId);
    
    List<TimelineItemEntity> findByEventId(UUID eventId);
    
    Optional<TimelineItemEntity> findByIdAndEventId(UUID id, UUID eventId);
    
    // Hierarchy queries
    List<TimelineItemEntity> findByEventIdAndParentTaskIdIsNullOrderByTaskOrderAsc(UUID eventId);
    
    List<TimelineItemEntity> findByParentTaskIdOrderByTaskOrderAsc(UUID parentTaskId);
    
    List<TimelineItemEntity> findByEventIdAndIsParentTaskTrue(UUID eventId);
    
    // Status queries
    List<TimelineItemEntity> findByEventIdAndStatus(UUID eventId, TimelineStatus status);
    
    long countByEventIdAndStatus(UUID eventId, TimelineStatus status);
    
    // Assignee queries
    List<TimelineItemEntity> findByEventIdAndAssignedTo(UUID eventId, UUID assignedTo);
    
    // Date range queries
    @Query("SELECT t FROM TimelineItemEntity t WHERE t.eventId = :eventId " +
           "AND t.startDate IS NOT NULL " +
           "AND t.startDate >= :startDate AND t.startDate <= :endDate " +
           "ORDER BY t.startDate ASC")
    List<TimelineItemEntity> findByEventIdAndDateRange(
        @Param("eventId") UUID eventId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT t FROM TimelineItemEntity t WHERE t.eventId = :eventId " +
           "AND t.dueDate IS NOT NULL " +
           "AND t.dueDate < :currentDate " +
           "AND t.status NOT IN (ai.eventplanner.common.domain.enums.TimelineStatus.COMPLETED, " +
           "ai.eventplanner.common.domain.enums.TimelineStatus.DONE, " +
           "ai.eventplanner.common.domain.enums.TimelineStatus.CANCELLED) " +
           "ORDER BY t.dueDate ASC")
    List<TimelineItemEntity> findOverdueTasks(
        @Param("eventId") UUID eventId,
        @Param("currentDate") LocalDateTime currentDate
    );
    
    @Query("SELECT t FROM TimelineItemEntity t WHERE t.eventId = :eventId " +
           "AND t.dueDate IS NOT NULL " +
           "AND t.dueDate >= :currentDate " +
           "AND t.dueDate <= :futureDate " +
           "AND t.status NOT IN (ai.eventplanner.common.domain.enums.TimelineStatus.COMPLETED, " +
           "ai.eventplanner.common.domain.enums.TimelineStatus.DONE) " +
           "ORDER BY t.dueDate ASC")
    List<TimelineItemEntity> findUpcomingTasks(
        @Param("eventId") UUID eventId,
        @Param("currentDate") LocalDateTime currentDate,
        @Param("futureDate") LocalDateTime futureDate
    );
    
    // Category and priority queries
    List<TimelineItemEntity> findByEventIdAndCategory(UUID eventId, String category);
    
    List<TimelineItemEntity> findByEventIdAndPriority(UUID eventId, String priority);
    
    // Search queries
    @Query("SELECT t FROM TimelineItemEntity t WHERE t.eventId = :eventId " +
           "AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY t.startDate ASC")
    List<TimelineItemEntity> searchByEventIdAndQuery(
        @Param("eventId") UUID eventId,
        @Param("query") String query
    );
    
    // Progress and statistics queries
    @Query("SELECT COUNT(t) FROM TimelineItemEntity t WHERE t.eventId = :eventId")
    long countByEventId(@Param("eventId") UUID eventId);
    
    @Query("SELECT COALESCE(AVG(t.progressPercentage), 0) FROM TimelineItemEntity t " +
           "WHERE t.eventId = :eventId AND t.isParentTask = true")
    Double getAverageProgressForEvent(@Param("eventId") UUID eventId);
    
    @Query("SELECT MIN(t.startDate) FROM TimelineItemEntity t WHERE t.eventId = :eventId AND t.startDate IS NOT NULL")
    LocalDateTime getEarliestDate(@Param("eventId") UUID eventId);
    
    @Query("SELECT MAX(COALESCE(t.endTime, t.dueDate, t.startDate)) FROM TimelineItemEntity t " +
           "WHERE t.eventId = :eventId AND (t.endTime IS NOT NULL OR t.dueDate IS NOT NULL OR t.startDate IS NOT NULL)")
    LocalDateTime getLatestDate(@Param("eventId") UUID eventId);
    
    // Note: Dependencies checking handled in service layer due to UUID array limitations
}


