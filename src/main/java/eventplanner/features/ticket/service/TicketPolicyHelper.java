package eventplanner.features.ticket.service;

import eventplanner.common.exception.exceptions.ApiException;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ErrorCode;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.ticket.dto.request.IssueTicketRequest;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.util.AuthValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Shared ticket policy helpers used by TicketService, TicketApprovalService,
 * and TicketWaitlistService — eliminates three copies of the same logic.
 */
@Component
@RequiredArgsConstructor
public class TicketPolicyHelper {

    public static final int DEFAULT_MAX_TICKETS_PER_PERSON = 5;

    private final TicketRepository ticketRepository;
    private final AttendeeRepository attendeeRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;

    /**
     * Resolve the effective max-tickets-per-person limit from a TicketType.
     * Returns {@link Integer#MAX_VALUE} when the type has no limit.
     */
    public int resolveMaxTicketsPerPerson(TicketType ticketType) {
        Integer configuredMax = ticketType.getMaxTicketsPerPerson();
        int max = configuredMax == null ? Integer.MAX_VALUE : configuredMax;
        if (configuredMax != null && configuredMax <= 0) {
            max = DEFAULT_MAX_TICKETS_PER_PERSON;
        }
        return max;
    }

    /**
     * Count how many tickets a person already holds for an event,
     * looking up by attendee (via userId) first, then by email.
     */
    public long countExistingTickets(UUID eventId, UUID requesterId, String requesterEmail) {
        Attendee attendee = requesterId != null
            ? attendeeRepository.findByEventIdAndUserId(eventId, requesterId).orElse(null)
            : null;
        if (attendee != null) {
            return ticketRepository.findByAttendeeIdAndEventId(attendee.getId(), eventId).size();
        }
        if (requesterEmail != null) {
            return ticketRepository.findByOwnerEmailAndEventId(requesterEmail, eventId).size();
        }
        return 0;
    }

    /**
     * Enforce max-tickets-per-person limit — throws if adding {@code requestedQty}
     * would exceed the configured maximum.
     */
    public void enforceMaxTicketsPerPerson(TicketType ticketType, UUID eventId,
                                           UUID requesterId, String requesterEmail,
                                           int requestedQty) {
        int max = resolveMaxTicketsPerPerson(ticketType);
        if (max == Integer.MAX_VALUE) {
            return;
        }
        long existing = countExistingTickets(eventId, requesterId, requesterEmail);
        if (existing + requestedQty > max) {
            throw new ApiException(ErrorCode.MAX_TICKETS_EXCEEDED,
                "Cannot request more than " + max + " tickets per person");
        }
    }

    /**
     * Build an {@link IssueTicketRequest} from requester info — shared between
     * approval-flow and waitlist-flow.
     *
     * @param eventId       the event UUID
     * @param ticketTypeId  the ticket type UUID
     * @param quantity      how many tickets to issue
     * @param requester     the user account (nullable for guest entries)
     * @param rawEmail      the requester's email (may not be normalised)
     * @param rawName       the requester's display name
     * @param sendEmail     whether to send email notification
     * @param sendPush      whether to send push notification
     */
    public IssueTicketRequest buildIssueRequest(UUID eventId, UUID ticketTypeId, int quantity,
                                                UserAccount requester,
                                                String rawEmail, String rawName,
                                                boolean sendEmail, boolean sendPush) {
        String email = normalizeEmail(rawEmail);
        String name = rawName;
        if (email == null || email.isBlank()) {
            if (requester != null) {
                email = normalizeEmail(requester.getEmail());
            }
        }
        if (name == null || name.isBlank()) {
            name = requester != null ? requester.getName() : email;
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Requester email is required to issue tickets");
        }

        Attendee attendee = null;
        if (requester != null && requester.getId() != null) {
            attendee = attendeeRepository.findByEventIdAndUserId(eventId, requester.getId()).orElse(null);
        }
        if (attendee == null && email != null) {
            attendee = attendeeRepository.findByEventIdAndEmailIgnoreCase(eventId, email).orElse(null);
        }

        IssueTicketRequest req = new IssueTicketRequest();
        req.setEventId(eventId);
        req.setTicketTypeId(ticketTypeId);
        req.setQuantity(quantity);
        if (attendee != null) {
            req.setAttendeeId(attendee.getId());
        } else {
            req.setOwnerEmail(email);
            req.setOwnerName(name);
        }
        req.setSendEmail(sendEmail);
        req.setSendPushNotification(sendPush);
        return req;
    }

    // ---- Entity loading helpers (eliminates duplication between WaitlistService & ApprovalService) ----

    /**
     * Load an event by ID or throw {@link ResourceNotFoundException}.
     */
    public Event loadEvent(UUID eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }

    /**
     * Load a ticket type, verifying it belongs to the given event.
     */
    public TicketType loadTicketType(Event event, UUID ticketTypeId) {
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + ticketTypeId));
        if (ticketType.getEvent() == null || !Objects.equals(ticketType.getEvent().getId(), event.getId())) {
            throw new BadRequestException("Ticket type does not belong to this event");
        }
        return ticketType;
    }

    /**
     * Guard: ensure the ticket type is currently on sale.
     */
    public void ensureTicketTypeOnSale(TicketType ticketType) {
        if (!ticketType.isOnSale()) {
            throw new BadRequestException("Ticket sales are not active for this ticket type");
        }
    }

    /**
     * Null-safe email normalisation delegating to the central utility.
     */
    public static String normalizeEmail(String email) {
        return email != null ? AuthValidationUtil.normalizeEmail(email) : null;
    }
}
