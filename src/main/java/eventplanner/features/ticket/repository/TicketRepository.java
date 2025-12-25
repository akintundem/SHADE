package eventplanner.features.ticket.repository;

import eventplanner.features.ticket.entity.Ticket;
import eventplanner.features.ticket.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Ticket entity with query support for validation and filtering.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    
    /**
     * Find ticket by unique ticket number (for QR validation).
     */
    @Query("SELECT t FROM Ticket t WHERE t.ticketNumber = :ticketNumber")
    Optional<Ticket> findByTicketNumber(@Param("ticketNumber") String ticketNumber);
    
    /**
     * Find ticket by QR code data (for validation).
     */
    @Query("SELECT t FROM Ticket t WHERE t.qrCodeData = :qrCodeData")
    Optional<Ticket> findByQrCodeData(@Param("qrCodeData") String qrCodeData);
    
    /**
     * Find all tickets for an attendee.
     */
    @Query("SELECT t FROM Ticket t WHERE t.attendee.id = :attendeeId")
    List<Ticket> findByAttendeeId(@Param("attendeeId") UUID attendeeId);
    
    /**
     * Find tickets for an attendee and event.
     */
    @Query("SELECT t FROM Ticket t WHERE t.attendee.id = :attendeeId AND t.event.id = :eventId")
    List<Ticket> findByAttendeeIdAndEventId(@Param("attendeeId") UUID attendeeId, @Param("eventId") UUID eventId);
    
    /**
     * Find tickets for an event with pagination.
     */
    @Query("SELECT t FROM Ticket t WHERE t.event.id = :eventId")
    Page<Ticket> findByEventId(@Param("eventId") UUID eventId, Pageable pageable);
    
    /**
     * Find tickets for an event by status.
     */
    @Query("SELECT t FROM Ticket t WHERE t.event.id = :eventId AND t.status = :status")
    List<Ticket> findByEventIdAndStatus(@Param("eventId") UUID eventId, @Param("status") TicketStatus status);
    
    /**
     * Find tickets for an event by status with pagination.
     */
    @Query("SELECT t FROM Ticket t WHERE t.event.id = :eventId AND t.status = :status")
    Page<Ticket> findByEventIdAndStatus(@Param("eventId") UUID eventId, @Param("status") TicketStatus status, Pageable pageable);
    
    /**
     * Find tickets by ticket type with pagination.
     */
    @Query("SELECT t FROM Ticket t WHERE t.ticketType.id = :ticketTypeId")
    Page<Ticket> findByTicketTypeId(@Param("ticketTypeId") UUID ticketTypeId, Pageable pageable);
    
    /**
     * Count tickets by ticket type and status.
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.ticketType.id = :ticketTypeId AND t.status = :status")
    long countByTicketTypeIdAndStatus(@Param("ticketTypeId") UUID ticketTypeId, @Param("status") TicketStatus status);
    
    /**
     * Find ticket with pessimistic lock for atomic updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdForUpdate(@Param("id") UUID id);
    
    /**
     * Count tickets for an event by status.
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.id = :eventId AND t.status = :status")
    long countByEventIdAndStatus(@Param("eventId") UUID eventId, @Param("status") TicketStatus status);
    
    /**
     * Find tickets for an attendee filtered by status.
     */
    @Query("SELECT t FROM Ticket t WHERE t.attendee.id = :attendeeId AND t.status = :status")
    List<Ticket> findByAttendeeIdAndStatus(@Param("attendeeId") UUID attendeeId, @Param("status") TicketStatus status);

    /**
     * Find tickets by owner email (for email-only tickets).
     */
    @Query("SELECT t FROM Ticket t WHERE t.ownerEmail = :email AND t.event.id = :eventId")
    List<Ticket> findByOwnerEmailAndEventId(@Param("email") String email, @Param("eventId") UUID eventId);

    /**
     * Find tickets by owner email (all events).
     */
    @Query("SELECT t FROM Ticket t WHERE t.ownerEmail = :email")
    List<Ticket> findByOwnerEmail(@Param("email") String email);

    /**
     * Find all pending tickets that have expired (more than 15 minutes old).
     * Used by scheduled task to automatically expire pending reservations.
     */
    @Query("SELECT t FROM Ticket t WHERE t.status = 'PENDING' AND t.pendingAt IS NOT NULL AND t.pendingAt < :expirationTime")
    List<Ticket> findExpiredPendingTickets(@Param("expirationTime") LocalDateTime expirationTime);
}

