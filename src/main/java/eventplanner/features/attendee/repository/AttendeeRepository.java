package eventplanner.features.attendee.repository;

import eventplanner.common.domain.enums.VisibilityLevel;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Attendee entity with pagination and filtering support.
 */
@Repository
public interface AttendeeRepository extends JpaRepository<Attendee, UUID> {

    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId")
    List<Attendee> findByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT a FROM Attendee a WHERE a.id = :id AND a.event.id = :eventId")
    Optional<Attendee> findByIdAndEventId(@Param("id") UUID id, @Param("eventId") UUID eventId);

    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId")
    Page<Attendee> findByEventId(@Param("eventId") UUID eventId, Pageable pageable);

    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.rsvpStatus IN :statuses")
    Page<Attendee> findByEventIdAndRsvpStatusIn(@Param("eventId") UUID eventId,
                                                 @Param("statuses") List<AttendeeStatus> statuses,
                                                 Pageable pageable);

    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.checkedInAt IS NOT NULL")
    Page<Attendee> findCheckedInByEventId(@Param("eventId") UUID eventId, Pageable pageable);

    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.checkedInAt IS NULL")
    Page<Attendee> findNotCheckedInByEventId(@Param("eventId") UUID eventId, Pageable pageable);

    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND (LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Attendee> searchByEventIdAndEmailOrName(@Param("eventId") UUID eventId,
                                                  @Param("search") String search,
                                                  Pageable pageable);

    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.email = :email")
    Optional<Attendee> findByEventIdAndEmail(@Param("eventId") UUID eventId, @Param("email") String email);

    /**
     * Case-insensitive lookup by email to prevent duplicate attendees with different casing.
     */
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND LOWER(a.email) = LOWER(:email)")
    Optional<Attendee> findByEventIdAndEmailIgnoreCase(@Param("eventId") UUID eventId, @Param("email") String email);

    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.user.id = :userId")
    Optional<Attendee> findByEventIdAndUserId(@Param("eventId") UUID eventId, @Param("userId") UUID userId);

    /**
     * Find all attendees for an event with PUBLIC participation visibility.
     * Used for public-facing attendee lists where users want to hide their participation.
     */
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.participationVisibility = :visibility")
    Page<Attendee> findByEventIdAndParticipationVisibility(@Param("eventId") UUID eventId,
                                                           @Param("visibility") VisibilityLevel visibility,
                                                           Pageable pageable);

    /**
     * Find all attendees for a specific user across all events.
     * Used for showing a user's event history/participation.
     */
    @Query("SELECT a FROM Attendee a WHERE a.user.id = :userId")
    List<Attendee> findByUserId(@Param("userId") UUID userId);

    /**
     * Find all PUBLIC attendees for a specific user across all events.
     * Used for showing a user's public event history.
     */
    @Query("SELECT a FROM Attendee a WHERE a.user.id = :userId AND a.participationVisibility = :visibility")
    List<Attendee> findByUserIdAndParticipationVisibility(@Param("userId") UUID userId,
                                                          @Param("visibility") VisibilityLevel visibility);
}
