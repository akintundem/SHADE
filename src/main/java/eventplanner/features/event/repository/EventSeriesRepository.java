package eventplanner.features.event.repository;

import eventplanner.features.event.entity.EventSeries;
import eventplanner.features.event.enums.RecurrencePattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventSeriesRepository extends JpaRepository<EventSeries, UUID>, JpaSpecificationExecutor<EventSeries> {

    /**
     * Find series by owner ID with pagination.
     */
    Page<EventSeries> findByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Find active series by owner ID.
     */
    Page<EventSeries> findByOwnerIdAndIsActiveTrue(UUID ownerId, Pageable pageable);

    /**
     * Find series by name (case-insensitive) for a specific owner.
     */
    Optional<EventSeries> findByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

    /**
     * Find all active series that need occurrence generation.
     * Used by the scheduler to auto-generate future events.
     */
    @Query("SELECT s FROM EventSeries s WHERE s.isActive = true AND s.autoGenerate = true " +
           "AND (s.recurrenceEndType = 'NEVER' " +
           "     OR (s.recurrenceEndType = 'BY_DATE' AND s.seriesEndDate > :now) " +
           "     OR (s.recurrenceEndType = 'BY_OCCURRENCES' AND s.occurrencesGenerated < s.maxOccurrences))")
    List<EventSeries> findActiveSeriesNeedingGeneration(@Param("now") LocalDateTime now);

    /**
     * Find series by recurrence pattern.
     */
    List<EventSeries> findByRecurrencePatternAndIsActiveTrue(RecurrencePattern pattern);

    /**
     * Count active series by owner.
     */
    long countByOwnerIdAndIsActiveTrue(UUID ownerId);

    /**
     * Find series with upcoming events that need generation within the specified horizon.
     */
    @Query("SELECT DISTINCT s FROM EventSeries s " +
           "LEFT JOIN s.events e " +
           "WHERE s.isActive = true AND s.autoGenerate = true " +
           "AND (SELECT MAX(ev.startDateTime) FROM Event ev WHERE ev.parentSeries = s) < :horizon " +
           "OR NOT EXISTS (SELECT 1 FROM Event ev WHERE ev.parentSeries = s)")
    List<EventSeries> findSeriesNeedingOccurrencesBeforeHorizon(@Param("horizon") LocalDateTime horizon);

    /**
     * Update the occurrences generated count.
     */
    @Modifying
    @Query("UPDATE EventSeries s SET s.occurrencesGenerated = :count WHERE s.id = :seriesId")
    int updateOccurrencesGenerated(@Param("seriesId") UUID seriesId, @Param("count") Integer count);

    /**
     * Increment the occurrences generated count.
     */
    @Modifying
    @Query("UPDATE EventSeries s SET s.occurrencesGenerated = s.occurrencesGenerated + 1 WHERE s.id = :seriesId")
    int incrementOccurrencesGenerated(@Param("seriesId") UUID seriesId);

    /**
     * Deactivate a series.
     */
    @Modifying
    @Query("UPDATE EventSeries s SET s.isActive = false WHERE s.id = :seriesId")
    int deactivateSeries(@Param("seriesId") UUID seriesId);

    /**
     * Find series by master event ID.
     */
    Optional<EventSeries> findByMasterEventId(UUID masterEventId);

    /**
     * Check if a series exists and is owned by a specific user.
     */
    boolean existsByIdAndOwnerId(UUID seriesId, UUID ownerId);

    /**
     * Find all series starting within a date range.
     */
    @Query("SELECT s FROM EventSeries s WHERE s.seriesStartDate BETWEEN :start AND :end")
    List<EventSeries> findSeriesStartingBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
