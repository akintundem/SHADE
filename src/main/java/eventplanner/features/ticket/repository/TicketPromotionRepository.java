package eventplanner.features.ticket.repository;

import eventplanner.features.ticket.entity.TicketPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface TicketPromotionRepository extends JpaRepository<TicketPromotion, UUID> {

    Optional<TicketPromotion> findByEventIdAndCodeIgnoreCase(UUID eventId, String code);
    Optional<TicketPromotion> findByEventIdAndTicketTypeIdAndCodeIgnoreCase(UUID eventId, UUID ticketTypeId, String code);

    List<TicketPromotion> findByEventId(UUID eventId);
    List<TicketPromotion> findByEventIdAndTicketTypeId(UUID eventId, UUID ticketTypeId);
}
