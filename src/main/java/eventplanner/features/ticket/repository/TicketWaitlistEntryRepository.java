package eventplanner.features.ticket.repository;

import eventplanner.features.ticket.entity.TicketWaitlistEntry;
import eventplanner.features.ticket.enums.TicketWaitlistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketWaitlistEntryRepository extends JpaRepository<TicketWaitlistEntry, UUID> {

    Optional<TicketWaitlistEntry> findByIdAndEventId(UUID id, UUID eventId);

    Page<TicketWaitlistEntry> findByEventId(UUID eventId, Pageable pageable);

    Page<TicketWaitlistEntry> findByEventIdAndStatus(UUID eventId, TicketWaitlistStatus status, Pageable pageable);

    List<TicketWaitlistEntry> findByRequesterIdAndEventId(UUID requesterId, UUID eventId);

    boolean existsByEventIdAndTicketTypeIdAndRequesterIdAndStatus(UUID eventId, UUID ticketTypeId, UUID requesterId, TicketWaitlistStatus status);

    List<TicketWaitlistEntry> findByEventIdAndTicketTypeIdAndStatusOrderByCreatedAtAsc(UUID eventId, UUID ticketTypeId, TicketWaitlistStatus status);
}
