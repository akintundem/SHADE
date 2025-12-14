package eventplanner.features.attendee.repository;

import eventplanner.features.attendee.entity.Attendee;
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
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId")
    List<Attendee> findByEventId(@Param("eventId") UUID eventId);
    
    @Query("SELECT a FROM Attendee a WHERE a.id = :id AND a.event.id = :eventId")
    Optional<Attendee> findByIdAndEventId(@Param("id") UUID id, @Param("eventId") UUID eventId);
    
    // Pagination support
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId")
    Page<Attendee> findByEventId(@Param("eventId") UUID eventId, Pageable pageable);
    
    // Filtering by status
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.rsvpStatus = :status")
    List<Attendee> findByEventIdAndRsvpStatus(@Param("eventId") UUID eventId, @Param("status") Attendee.Status status);
    
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.rsvpStatus = :status")
    Page<Attendee> findByEventIdAndRsvpStatus(@Param("eventId") UUID eventId, @Param("status") Attendee.Status status, Pageable pageable);
    
    // Filtering by multiple statuses
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.rsvpStatus IN :statuses")
    Page<Attendee> findByEventIdAndRsvpStatusIn(@Param("eventId") UUID eventId, 
                                                  @Param("statuses") List<Attendee.Status> statuses, 
                                                  Pageable pageable);
    
    // Check-in filtering
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.checkedInAt IS NOT NULL")
    Page<Attendee> findCheckedInByEventId(@Param("eventId") UUID eventId, Pageable pageable);
    
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.checkedInAt IS NULL")
    Page<Attendee> findNotCheckedInByEventId(@Param("eventId") UUID eventId, Pageable pageable);
    
    // Email/phone search
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND (LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Attendee> searchByEventIdAndEmailOrName(@Param("eventId") UUID eventId, 
                                                   @Param("search") String search, 
                                                   Pageable pageable);
    
    // Count queries
    @Query("SELECT COUNT(a) FROM Attendee a WHERE a.event.id = :eventId")
    Long countByEventId(@Param("eventId") UUID eventId);
    
    @Query("SELECT COUNT(a) FROM Attendee a WHERE a.event.id = :eventId AND a.rsvpStatus = :status")
    Long countByEventIdAndRsvpStatus(@Param("eventId") UUID eventId, @Param("status") Attendee.Status status);
    
    @Query("SELECT COUNT(a) FROM Attendee a WHERE a.event.id = :eventId AND a.checkedInAt IS NOT NULL")
    Long countCheckedInByEventId(@Param("eventId") UUID eventId);
    
    // Duplicate detection
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.email = :email")
    Optional<Attendee> findByEventIdAndEmail(@Param("eventId") UUID eventId, @Param("email") String email);
    
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.email IN :emails")
    List<Attendee> findByEventIdAndEmailIn(@Param("eventId") UUID eventId, @Param("emails") List<String> emails);
    
    // Find by user (for invite acceptance)
    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.user.id = :userId")
    Optional<Attendee> findByEventIdAndUserId(@Param("eventId") UUID eventId, @Param("userId") UUID userId);
}


