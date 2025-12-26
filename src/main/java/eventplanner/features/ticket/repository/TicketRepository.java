package eventplanner.features.ticket.repository;

import eventplanner.features.ticket.entity.Ticket;
import eventplanner.features.ticket.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Find tickets by owner email and event (for email-only tickets).
     */
    @Query("SELECT t FROM Ticket t WHERE t.ownerEmail = :email AND t.event.id = :eventId")
    List<Ticket> findByOwnerEmailAndEventId(@Param("email") String email, @Param("eventId") UUID eventId);

    /**
     * Find all pending tickets that have expired (more than 15 minutes old).
     * Used by scheduled task to automatically expire pending reservations.
     */
    @Query("SELECT t FROM Ticket t WHERE t.status = 'PENDING' AND t.pendingAt IS NOT NULL AND t.pendingAt < :expirationTime")
    List<Ticket> findExpiredPendingTickets(@Param("expirationTime") LocalDateTime expirationTime);

    /**
     * Find tickets for an event with optional status and ticket type filters.
     */
    @Query("SELECT t FROM Ticket t WHERE t.event.id = :eventId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:ticketTypeId IS NULL OR t.ticketType.id = :ticketTypeId)")
    Page<Ticket> findByEventIdWithFilters(
        @Param("eventId") UUID eventId,
        @Param("status") TicketStatus status,
        @Param("ticketTypeId") UUID ticketTypeId,
        Pageable pageable);

    /**
     * Find a specific ticket by ID and event ID.
     */
    @Query("SELECT t FROM Ticket t WHERE t.id = :ticketId AND t.event.id = :eventId")
    Optional<Ticket> findByIdAndEventId(@Param("ticketId") UUID ticketId, @Param("eventId") UUID eventId);

    /**
     * Check if a user has a valid ticket (ISSUED or VALIDATED) for an event via attendee relationship.
     * @param eventId The event ID
     * @param userId The user ID (matches attendee.user.id)
     * @return true if user has at least one valid ticket
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Ticket t " +
           "WHERE t.event.id = :eventId " +
           "AND t.attendee.user.id = :userId " +
           "AND t.status IN ('ISSUED', 'VALIDATED')")
    boolean hasValidTicketByUserId(@Param("eventId") UUID eventId, @Param("userId") UUID userId);

    /**
     * Check if a user has a valid ticket (ISSUED or VALIDATED) for an event via owner email.
     * @param eventId The event ID
     * @param email The owner email
     * @return true if user has at least one valid ticket
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Ticket t " +
           "WHERE t.event.id = :eventId " +
           "AND LOWER(t.ownerEmail) = LOWER(:email) " +
           "AND t.status IN ('ISSUED', 'VALIDATED')")
    boolean hasValidTicketByEmail(@Param("eventId") UUID eventId, @Param("email") String email);

    /**
     * Find valid tickets for a user and event (via attendee relationship).
     * @param eventId The event ID
     * @param userId The user ID
     * @return List of valid tickets
     */
    @Query("SELECT t FROM Ticket t " +
           "WHERE t.event.id = :eventId " +
           "AND t.attendee.user.id = :userId " +
           "AND t.status IN ('ISSUED', 'VALIDATED')")
    List<Ticket> findValidTicketsByUserId(@Param("eventId") UUID eventId, @Param("userId") UUID userId);
}
