package eventplanner.features.event.service;

import eventplanner.features.event.entity.Event;
import eventplanner.features.event.enums.EventStatus;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.ticket.entity.Ticket;
import eventplanner.features.ticket.enums.TicketStatus;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

/**
 * Scheduled service for automatic event status transitions.
 * Automatically updates event statuses based on event start/end times.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventStatusAutomationService {

    private final EventRepository eventRepository;

    @Autowired(required = false)
    private TicketRepository ticketRepository;

    @Autowired(required = false)
    private TicketTypeRepository ticketTypeRepository;

    /**
     * Scheduled task to automatically transition events to IN_PROGRESS.
     * Runs every 5 minutes to check for events that should start.
     * Cron expression format: second, minute, hour, day, month, weekday
     * Default cron: runs at 0 seconds of every 5 minutes
     */
    @Scheduled(cron = "${event.status.auto-transition.cron:0 */5 * * * *}")
    @Transactional
    public void transitionToInProgress() {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            
            // Find events that should be IN_PROGRESS:
            // - Status is PUBLISHED or REGISTRATION_OPEN or REGISTRATION_CLOSED
            // - startDateTime is in the past or now
            // - endDateTime is in the future (event hasn't ended yet)
            List<Event> eventsToStart = eventRepository.findAll((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // Status must be one that can transition to IN_PROGRESS
                predicates.add(cb.or(
                    cb.equal(root.get("eventStatus"), EventStatus.PUBLISHED),
                    cb.equal(root.get("eventStatus"), EventStatus.REGISTRATION_OPEN),
                    cb.equal(root.get("eventStatus"), EventStatus.REGISTRATION_CLOSED)
                ));
                
                // Start time must be in the past or now
                predicates.add(cb.lessThanOrEqualTo(root.get("startDateTime"), now));
                
                // End time must be in the future (event hasn't ended)
                predicates.add(cb.greaterThan(root.get("endDateTime"), now));
                
                // Not already archived
                predicates.add(cb.equal(root.get("isArchived"), false));
                
                return cb.and(predicates.toArray(new Predicate[0]));
            });

            if (eventsToStart.isEmpty()) {
                return;
            }

            log.info("Transitioning {} events to IN_PROGRESS", eventsToStart.size());

            int transitioned = 0;
            for (Event event : eventsToStart) {
                try {
                    EventStatus previousStatus = event.getEventStatus();
                    event.setEventStatus(EventStatus.IN_PROGRESS);
                    eventRepository.save(event);
                    transitioned++;
                    log.debug("Event '{}' (ID: {}) transitioned from {} to IN_PROGRESS", 
                            event.getName(), event.getId(), previousStatus);
                } catch (Exception e) {
                    log.error("Error transitioning event {} to IN_PROGRESS: {}", 
                            event.getId(), e.getMessage(), e);
                }
            }

            if (transitioned > 0) {
                log.info("Successfully transitioned {} events to IN_PROGRESS", transitioned);
            }
        } catch (Exception e) {
            log.error("Error in transitionToInProgress scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled task to automatically transition events to COMPLETED.
     * Runs every hour to check for events that have ended.
     * Cron expression: "0 0 * * * *" means: at 0 seconds of every hour
     */
    @Scheduled(cron = "${event.status.auto-complete.cron:0 0 * * * *}")
    @Transactional
    public void transitionToCompleted() {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            // Events are considered completed 24 hours after end time
            LocalDateTime completionThreshold = now.minusHours(24);
            
            // Find events that should be COMPLETED:
            // - Status is IN_PROGRESS or REGISTRATION_CLOSED
            // - endDateTime + 24 hours is in the past
            // - Not already archived
            List<Event> eventsToComplete = eventRepository.findAll((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // Status must be one that can transition to COMPLETED
                predicates.add(cb.or(
                    cb.equal(root.get("eventStatus"), EventStatus.IN_PROGRESS),
                    cb.equal(root.get("eventStatus"), EventStatus.REGISTRATION_CLOSED)
                ));
                
                // End time + 24 hours must be in the past
                predicates.add(cb.lessThanOrEqualTo(root.get("endDateTime"), completionThreshold));
                
                // Not already archived
                predicates.add(cb.equal(root.get("isArchived"), false));
                
                return cb.and(predicates.toArray(new Predicate[0]));
            });

            if (eventsToComplete.isEmpty()) {
                return;
            }

            log.info("Transitioning {} events to COMPLETED", eventsToComplete.size());

            int completed = 0;
            for (Event event : eventsToComplete) {
                try {
                    EventStatus previousStatus = event.getEventStatus();
                    event.setEventStatus(EventStatus.COMPLETED);
                    eventRepository.save(event);

                    // Expire pending tickets when event completes
                    if (ticketRepository != null && ticketTypeRepository != null) {
                        try {
                            List<Ticket> pendingTickets = ticketRepository.findByEventIdAndStatus(
                                event.getId(), TicketStatus.PENDING);

                            if (!pendingTickets.isEmpty()) {
                                for (Ticket ticket : pendingTickets) {
                                    ticket.cancel("Event completed - pending ticket expired");

                                    // Release reserved inventory
                                    if (ticket.getTicketType() != null) {
                                        ticketTypeRepository.decrementQuantityReserved(
                                            ticket.getTicketType().getId(), 1);
                                    }
                                }
                                ticketRepository.saveAll(pendingTickets);
                                log.debug("Expired {} pending tickets for completed event '{}'",
                                    pendingTickets.size(), event.getName());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to expire pending tickets for event {}: {}",
                                event.getId(), e.getMessage());
                        }
                    }

                    completed++;
                    log.debug("Event '{}' (ID: {}) transitioned from {} to COMPLETED",
                            event.getName(), event.getId(), previousStatus);
                } catch (Exception e) {
                    log.error("Error transitioning event {} to COMPLETED: {}",
                            event.getId(), e.getMessage(), e);
                }
            }

            if (completed > 0) {
                log.info("Successfully transitioned {} events to COMPLETED", completed);
            }
        } catch (Exception e) {
            log.error("Error in transitionToCompleted scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Manually trigger status transitions for testing or manual intervention.
     */
    @Transactional
    public void triggerStatusTransitions() {
        log.info("Manual trigger: Starting status transitions");
        transitionToInProgress();
        transitionToCompleted();
    }
}
