package eventplanner.features.ticket.repository;

import eventplanner.features.ticket.entity.TicketCheckout;
import eventplanner.features.ticket.enums.TicketCheckoutStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * Repository for ticket checkout sessions.
 */
@Repository
public interface TicketCheckoutRepository extends JpaRepository<TicketCheckout, UUID> {

    @EntityGraph(attributePaths = {"items"})
    Optional<TicketCheckout> findWithItemsById(UUID id);

    List<TicketCheckout> findByStatusAndExpiresAtBefore(TicketCheckoutStatus status, LocalDateTime expiresAt);

    Page<TicketCheckout> findByPurchaserId(UUID purchaserId, Pageable pageable);
}
