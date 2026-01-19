package eventplanner.features.ticket.repository;

import eventplanner.features.ticket.entity.TicketCheckoutItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ticket checkout line items.
 */
@Repository
public interface TicketCheckoutItemRepository extends JpaRepository<TicketCheckoutItem, UUID> {

    List<TicketCheckoutItem> findByCheckoutId(UUID checkoutId);
}
