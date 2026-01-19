package eventplanner.features.ticket.repository;

import eventplanner.features.ticket.entity.TicketApprovalRequest;
import eventplanner.features.ticket.enums.TicketApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketApprovalRequestRepository extends JpaRepository<TicketApprovalRequest, UUID> {

    Optional<TicketApprovalRequest> findByIdAndEventId(UUID id, UUID eventId);

    Page<TicketApprovalRequest> findByEventId(UUID eventId, Pageable pageable);

    Page<TicketApprovalRequest> findByEventIdAndStatus(UUID eventId, TicketApprovalStatus status, Pageable pageable);

    List<TicketApprovalRequest> findByRequesterIdAndEventId(UUID requesterId, UUID eventId);

    boolean existsByEventIdAndTicketTypeIdAndRequesterIdAndStatus(UUID eventId, UUID ticketTypeId, UUID requesterId, TicketApprovalStatus status);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM TicketApprovalRequest r " +
           "WHERE r.event.id = :eventId AND r.requester.id = :requesterId AND r.status = :status")
    int sumQuantityByRequesterAndEventIdAndStatus(
        @Param("requesterId") UUID requesterId,
        @Param("eventId") UUID eventId,
        @Param("status") TicketApprovalStatus status);
}
