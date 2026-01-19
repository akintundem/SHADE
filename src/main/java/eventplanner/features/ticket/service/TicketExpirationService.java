package eventplanner.features.ticket.service;

import eventplanner.features.ticket.entity.Ticket;
import eventplanner.features.ticket.enums.TicketStatus;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled service to automatically expire pending tickets after 15 minutes.
 * This releases reserved ticket quantities back to available inventory.
 */
@Service
@RequiredArgsConstructor
public class TicketExpirationService {

    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;

    /**
     * Scheduled task that runs every minute to check and expire pending tickets.
     * Cron expression: second, minute, hour, day, month, weekday
     * "0 * * * * *" means: at 0 seconds of every minute
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expirePendingTickets() {
        try {
            // Calculate expiration time (15 minutes ago)
            LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(15);
            
            // Find all expired pending tickets
            List<Ticket> expiredTickets = ticketRepository.findExpiredPendingTickets(expirationTime);

            if (expiredTickets.isEmpty()) {
                return;
            }

            for (Ticket ticket : expiredTickets) {
                try {
                    // Cancel the expired ticket
                    ticket.cancel("Pending reservation expired (15 minute window exceeded)");
                    ticketRepository.save(ticket);

                    // Release reserved quantity back to available
                    if (ticket.getTicketType() != null) {
                        ticketTypeRepository.decrementQuantityReserved(
                            ticket.getTicketType().getId(), 1);
                    }
                } catch (Exception ignored) {
                    // Continue processing other tickets if one fails
                }
            }


        } catch (Exception e) {

        }
    }

    /**
     * Manually expire a specific pending ticket.
     * Useful for testing or manual intervention.
     */
    @Transactional
    public void expirePendingTicket(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new IllegalStateException(
                "Ticket is not in PENDING status. Current status: " + ticket.getStatus());
        }

        // Cancel the ticket
        ticket.cancel("Pending reservation expired (15 minute window exceeded)");
        ticketRepository.save(ticket);

        // Release reserved quantity
        if (ticket.getTicketType() != null && ticket.getTicketType().getPriceMinor() != null) {
            ticketTypeRepository.decrementQuantityReserved(ticket.getTicketType().getId(), 1);
        }


    }
}
