package eventplanner.features.attendee.repository;

import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.entity.AttendeeStatus;
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
 * Repository for Attendee entity with pagination and filtering support
 */
@Repository
public interface AttendeeRepository extends JpaRepository<Attendee, UUID> {
    
    // Basic queries
    List<Attendee> findByEventId(UUID eventId);
    
    Optional<Attendee> findByIdAndEventId(UUID id, UUID eventId);
    
    // Pagination support
    Page<Attendee> findByEventId(UUID eventId, Pageable pageable);
    
    // Filtering by status
    List<Attendee> findByEventIdAndRsvpStatus(UUID eventId, AttendeeStatus status);
    
    Page<Attendee> findByEventIdAndRsvpStatus(UUID eventId, AttendeeStatus status, Pageable pageable);
    
    // Filtering by multiple statuses
    @Query("SELECT a FROM Attendee a WHERE a.eventId = :eventId AND a.rsvpStatus IN :statuses")
    Page<Attendee> findByEventIdAndRsvpStatusIn(@Param("eventId") UUID eventId, 
                                                  @Param("statuses") List<AttendeeStatus> statuses, 
                                                  Pageable pageable);
    
    // Check-in filtering
    @Query("SELECT a FROM Attendee a WHERE a.eventId = :eventId AND a.checkedInAt IS NOT NULL")
    Page<Attendee> findCheckedInByEventId(@Param("eventId") UUID eventId, Pageable pageable);
    
    @Query("SELECT a FROM Attendee a WHERE a.eventId = :eventId AND a.checkedInAt IS NULL")
    Page<Attendee> findNotCheckedInByEventId(@Param("eventId") UUID eventId, Pageable pageable);
    
    // Email/phone search
    @Query("SELECT a FROM Attendee a WHERE a.eventId = :eventId AND (LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Attendee> searchByEventIdAndEmailOrName(@Param("eventId") UUID eventId, 
                                                   @Param("search") String search, 
                                                   Pageable pageable);
    
    // Count queries
    Long countByEventId(UUID eventId);
    
    Long countByEventIdAndRsvpStatus(UUID eventId, AttendeeStatus status);
    
    @Query("SELECT COUNT(a) FROM Attendee a WHERE a.eventId = :eventId AND a.checkedInAt IS NOT NULL")
    Long countCheckedInByEventId(@Param("eventId") UUID eventId);
    
    // Duplicate detection
    Optional<Attendee> findByEventIdAndEmail(UUID eventId, String email);
    
    List<Attendee> findByEventIdAndEmailIn(UUID eventId, List<String> emails);
}


