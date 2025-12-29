package eventplanner.features.ticket.service;

import eventplanner.features.ticket.entity.Ticket;
import eventplanner.features.ticket.entity.TicketCheckout;
import eventplanner.features.ticket.enums.TicketCheckoutStatus;
import eventplanner.features.ticket.enums.TicketStatus;
import eventplanner.features.ticket.repository.TicketCheckoutRepository;
import eventplanner.features.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scheduled task to expire checkout sessions and release reserved tickets.
 */
@Service
@RequiredArgsConstructor
public class TicketCheckoutExpirationService {

    private final TicketCheckoutRepository checkoutRepository;
    private final TicketRepository ticketRepository;
    private final TicketTypeService ticketTypeService;

    /**
     * Runs every minute to expire checkout sessions whose hold windows elapsed.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expireCheckouts() {
        LocalDateTime cutoff = LocalDateTime.now();
        List<TicketCheckout> expired = checkoutRepository.findByStatusAndExpiresAtBefore(
            TicketCheckoutStatus.PENDING_PAYMENT, cutoff);

        for (TicketCheckout checkout : expired) {
            try {
                expireCheckout(checkout);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Manually expire a checkout (useful for tests or admin tooling).
     */
    @Transactional
    public void expireCheckout(UUID checkoutId) {
        TicketCheckout checkout = checkoutRepository.findById(checkoutId)
            .orElseThrow(() -> new IllegalArgumentException("Checkout not found: " + checkoutId));
        expireCheckout(checkout);
    }

    private void expireCheckout(TicketCheckout checkout) {
        if (checkout.getStatus() != TicketCheckoutStatus.PENDING_PAYMENT) {
            return;
        }

        List<Ticket> tickets = ticketRepository.findByCheckoutId(checkout.getId());

        Map<UUID, Integer> quantitiesByType = new HashMap<>();
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.PENDING) {
                if (ticket.getTicketType() != null) {
                    quantitiesByType.merge(ticket.getTicketType().getId(), 1, (a, b) -> a + b);
                }
                ticket.cancel("Checkout expired (15 minute hold elapsed)");
            }
        }

        if (!tickets.isEmpty()) {
            ticketRepository.saveAll(tickets);
        }
        quantitiesByType.forEach((ticketTypeId, qty) -> ticketTypeService.releaseReservation(ticketTypeId, qty));

        checkout.setStatus(TicketCheckoutStatus.EXPIRED);
        checkout.setExpiresAt(null);
        checkoutRepository.save(checkout);
    }
}
