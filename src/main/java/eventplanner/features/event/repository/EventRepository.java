package eventplanner.features.event.repository;

import eventplanner.common.domain.enums.EventStatus;
import eventplanner.common.domain.enums.EventType;
import eventplanner.features.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    
    // Basic queries
    Page<Event> findByEventStatus(String status, Pageable pageable);
    Page<Event> findByOwnerId(UUID ownerId, Pageable pageable);
    
    // Event type and status queries
    List<Event> findByEventType(EventType eventType);
    List<Event> findByEventStatus(EventStatus eventStatus);
    List<Event> findByEventTypeAndEventStatus(EventType eventType, EventStatus eventStatus);
    
    // Public events
    List<Event> findByIsPublicTrue();
    List<Event> findByIsPublicTrueAndEventStatus(String eventStatus);
    
    // Date-based queries
    List<Event> findByStartDateTimeAfter(LocalDateTime dateTime);
    List<Event> findByStartDateTimeBefore(LocalDateTime dateTime);
    List<Event> findByStartDateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<Event> findByStartDateTimeAfterAndIsPublicTrue(LocalDateTime dateTime);
    
    // Capacity queries
    List<Event> findByCapacityGreaterThan(Integer capacity);
    List<Event> findByCurrentAttendeeCountLessThan(Integer count);
    
    // Search queries
    @Query("SELECT e FROM Event e WHERE " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.theme) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.hashtag) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Event> searchEvents(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT e FROM Event e WHERE " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.theme) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.hashtag) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND e.isPublic = true")
    List<Event> searchPublicEvents(@Param("searchTerm") String searchTerm);
    
    // Analytics queries
    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdAt BETWEEN :startDate AND :endDate")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT e.eventStatus, COUNT(e) FROM Event e WHERE e.createdAt BETWEEN :startDate AND :endDate GROUP BY e.eventStatus")
    Map<String, Long> getEventsByStatus(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT YEAR(e.createdAt) as year, MONTH(e.createdAt) as month, COUNT(e) as count FROM Event e WHERE e.createdAt BETWEEN :startDate AND :endDate GROUP BY YEAR(e.createdAt), MONTH(e.createdAt) ORDER BY YEAR(e.createdAt), MONTH(e.createdAt)")
    List<Map<String, Object>> getEventsByMonth(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    Long countByEventStatus(String status);
    
    // User relationship queries (these would need to be implemented with proper joins to EventUser/EventAttendance tables)
    @Query("SELECT e FROM Event e WHERE e.ownerId = :userId")
    List<Event> findEventsByOwner(@Param("userId") UUID userId);
    
    @Query("SELECT e FROM Event e WHERE e.ownerId = :userId AND e.startDateTime > :now")
    List<Event> findUpcomingEventsByOwner(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT e FROM Event e WHERE e.ownerId = :userId AND e.startDateTime < :now")
    List<Event> findPastEventsByOwner(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    // Featured and trending events (placeholder queries - would need more complex logic)
    @Query("SELECT e FROM Event e WHERE e.isPublic = true AND e.eventStatus = 'PUBLISHED' ORDER BY e.createdAt DESC")
    List<Event> findFeaturedEvents(Pageable pageable);
    
    @Query("SELECT e FROM Event e WHERE e.isPublic = true AND e.eventStatus = 'PUBLISHED' ORDER BY e.currentAttendeeCount DESC")
    List<Event> findTrendingEvents(Pageable pageable);
    
    // Filtering with pagination
    @Query("SELECT e FROM Event e WHERE " +
           "(:status IS NULL OR e.eventStatus = :status) AND " +
           "(:isPublic IS NULL OR e.isPublic = :isPublic) AND " +
           "(:startDateFrom IS NULL OR e.startDateTime >= :startDateFrom) AND " +
           "(:startDateTo IS NULL OR e.startDateTime <= :startDateTo) AND " +
           "(:isArchived IS NULL OR e.isArchived = :isArchived) AND " +
           "(:search IS NULL OR " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.hashtag) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.theme) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Event> findEventsWithFilters(
            @Param("status") EventStatus status,
            @Param("isPublic") Boolean isPublic,
            @Param("startDateFrom") LocalDateTime startDateFrom,
            @Param("startDateTo") LocalDateTime startDateTo,
            @Param("isArchived") Boolean isArchived,
            @Param("search") String search,
            Pageable pageable);
}
