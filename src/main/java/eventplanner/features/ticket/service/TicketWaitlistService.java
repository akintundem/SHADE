package eventplanner.features.ticket.service;

import eventplanner.common.exception.exceptions.ApiException;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ConflictException;
import eventplanner.common.exception.exceptions.ErrorCode;
import eventplanner.common.exception.exceptions.ForbiddenException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.ticket.dto.request.CreateTicketWaitlistRequest;
import eventplanner.features.ticket.dto.request.IssueTicketRequest;
import eventplanner.features.ticket.dto.request.TicketWaitlistFulfillRequest;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.entity.TicketWaitlistEntry;
import eventplanner.features.ticket.enums.TicketWaitlistStatus;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import eventplanner.features.ticket.repository.TicketWaitlistEntryRepository;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.util.AuthValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketWaitlistService {

    private static final int DEFAULT_MAX_TICKETS_PER_PERSON = 5;

    private final TicketWaitlistEntryRepository waitlistRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final AttendeeRepository attendeeRepository;
    private final TicketService ticketService;
    private final AuthorizationService authorizationService;
    private final TicketingPolicyService ticketingPolicyService;

    public TicketWaitlistEntry createEntry(UUID eventId, CreateTicketWaitlistRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (principal == null || principal.getUser() == null) {
            throw new BadRequestException("Authenticated user is required");
        }

        Event event = loadEvent(eventId);
        ticketingPolicyService.ensureEventOpenForTicketing(event);

        TicketType ticketType = loadTicketType(event, request.getTicketTypeId());
        ensureTicketTypeOnSale(ticketType);

        if (ticketType.canPurchase(request.getQuantity())) {
            throw new ConflictException("Tickets are available; purchase instead of joining the waitlist");
        }

        UUID requesterId = principal.getId();
        if (waitlistRepository.existsByEventIdAndTicketTypeIdAndRequesterIdAndStatus(
                eventId, ticketType.getId(), requesterId, TicketWaitlistStatus.WAITING)) {
            throw new ConflictException("You are already on the waitlist for this ticket type");
        }

        String requesterEmail = normalizeEmail(principal.getUser().getEmail());
        String requesterName = principal.getUser().getName() != null
            ? principal.getUser().getName().trim()
            : requesterEmail;
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new BadRequestException("Requester email is required");
        }

        int maxTicketsPerPerson = resolveMaxTicketsPerPerson(ticketType);
        long existingTickets = countExistingTickets(eventId, requesterId, requesterEmail);
        if (maxTicketsPerPerson != Integer.MAX_VALUE &&
            existingTickets + request.getQuantity() > maxTicketsPerPerson) {
            throw new ApiException(ErrorCode.MAX_TICKETS_EXCEEDED,
                "Cannot request more than " + maxTicketsPerPerson + " tickets per person");
        }

        TicketWaitlistEntry entry = new TicketWaitlistEntry();
        entry.setEvent(event);
        entry.setTicketType(ticketType);
        entry.setRequester(principal.getUser());
        entry.setRequesterEmail(requesterEmail);
        entry.setRequesterName(requesterName);
        entry.setQuantity(request.getQuantity());

        return waitlistRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<TicketWaitlistEntry> listEntries(UUID eventId, TicketWaitlistStatus status, Pageable pageable) {
        if (status == null) {
            return waitlistRepository.findByEventId(eventId, pageable);
        }
        return waitlistRepository.findByEventIdAndStatus(eventId, status, pageable);
    }

    @Transactional(readOnly = true)
    public List<TicketWaitlistEntry> listEntriesForUser(UUID eventId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Authenticated user is required");
        }
        return waitlistRepository.findByRequesterIdAndEventId(principal.getId(), eventId);
    }

    public TicketWaitlistEntry fulfillEntry(UUID eventId, UUID entryId, TicketWaitlistFulfillRequest request,
                                            UserPrincipal principal) {
        ensureCanManageWaitlist(principal, eventId);

        TicketWaitlistEntry entry = loadEntry(eventId, entryId);
        if (entry.getStatus() != TicketWaitlistStatus.WAITING) {
            throw new ConflictException("Waitlist entry is not waiting");
        }

        TicketType ticketType = entry.getTicketType();
        if (ticketType == null) {
            throw new BadRequestException("Waitlist entry is missing ticket type");
        }
        if (!ticketType.canPurchase(entry.getQuantity())) {
            throw new ApiException(ErrorCode.TICKET_TYPE_SOLD_OUT, "Not enough tickets available");
        }

        IssueTicketRequest issueRequest = buildIssueRequest(entry, request);
        ticketService.issueTickets(List.of(issueRequest), principal, true, true);

        entry.fulfill(principal.getUser());
        return waitlistRepository.save(entry);
    }

    public TicketWaitlistEntry cancelEntry(UUID eventId, UUID entryId, UserPrincipal principal) {
        TicketWaitlistEntry entry = loadEntry(eventId, entryId);
        if (!canModifyEntry(principal, entry)) {
            throw new ForbiddenException("Access denied to waitlist entry");
        }
        try {
            entry.cancel();
        } catch (IllegalStateException e) {
            throw new ConflictException(e.getMessage());
        }
        return waitlistRepository.save(entry);
    }

    private Event loadEvent(UUID eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }

    private TicketType loadTicketType(Event event, UUID ticketTypeId) {
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + ticketTypeId));
        if (ticketType.getEvent() == null || !Objects.equals(ticketType.getEvent().getId(), event.getId())) {
            throw new BadRequestException("Ticket type does not belong to this event");
        }
        return ticketType;
    }

    private TicketWaitlistEntry loadEntry(UUID eventId, UUID entryId) {
        return waitlistRepository.findByIdAndEventId(entryId, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found: " + entryId));
    }

    private void ensureTicketTypeOnSale(TicketType ticketType) {
        if (!ticketType.isOnSale()) {
            throw new BadRequestException("Ticket sales are not active for this ticket type");
        }
    }

    private void ensureCanManageWaitlist(UserPrincipal principal, UUID eventId) {
        if (principal == null || eventId == null) {
            throw new ForbiddenException("Access denied to waitlist");
        }
        if (authorizationService.isAdmin(principal) || authorizationService.isEventOwner(principal, eventId)) {
            return;
        }
        if (!authorizationService.hasEventMembership(principal, eventId)) {
            throw new ForbiddenException("Access denied to waitlist");
        }
    }

    private boolean canModifyEntry(UserPrincipal principal, TicketWaitlistEntry entry) {
        if (principal == null || entry == null || entry.getEvent() == null) {
            return false;
        }
        UUID requesterId = entry.getRequester() != null ? entry.getRequester().getId() : null;
        if (requesterId != null && requesterId.equals(principal.getId())) {
            return true;
        }
        UUID eventId = entry.getEvent().getId();
        return authorizationService.isAdmin(principal) ||
            authorizationService.isEventOwner(principal, eventId) ||
            authorizationService.hasEventMembership(principal, eventId);
    }

    private IssueTicketRequest buildIssueRequest(TicketWaitlistEntry entry, TicketWaitlistFulfillRequest request) {
        UserAccount requester = entry.getRequester();
        String requesterEmail = normalizeEmail(entry.getRequesterEmail());
        String requesterName = entry.getRequesterName();
        if (requesterEmail == null || requesterEmail.isBlank()) {
            if (requester != null) {
                requesterEmail = normalizeEmail(requester.getEmail());
            }
        }
        if (requesterName == null || requesterName.isBlank()) {
            requesterName = requester != null ? requester.getName() : requesterEmail;
        }
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new BadRequestException("Requester email is required to issue tickets");
        }

        Attendee attendee = null;
        if (requester != null && requester.getId() != null) {
            attendee = attendeeRepository.findByEventIdAndUserId(entry.getEvent().getId(), requester.getId()).orElse(null);
        }
        if (attendee == null && requesterEmail != null) {
            attendee = attendeeRepository.findByEventIdAndEmailIgnoreCase(entry.getEvent().getId(), requesterEmail).orElse(null);
        }

        IssueTicketRequest issueRequest = new IssueTicketRequest();
        issueRequest.setEventId(entry.getEvent().getId());
        issueRequest.setTicketTypeId(entry.getTicketType().getId());
        issueRequest.setQuantity(entry.getQuantity());
        if (attendee != null) {
            issueRequest.setAttendeeId(attendee.getId());
        } else {
            issueRequest.setOwnerEmail(requesterEmail);
            issueRequest.setOwnerName(requesterName);
        }
        boolean sendEmail = request == null || Boolean.TRUE.equals(request.getSendEmail());
        boolean sendPush = request == null || Boolean.TRUE.equals(request.getSendPush());
        issueRequest.setSendEmail(sendEmail);
        issueRequest.setSendPushNotification(sendPush);
        return issueRequest;
    }

    private String normalizeEmail(String email) {
        return email != null ? AuthValidationUtil.normalizeEmail(email) : null;
    }

    private int resolveMaxTicketsPerPerson(TicketType ticketType) {
        Integer configuredMax = ticketType.getMaxTicketsPerPerson();
        int maxTicketsPerPerson = configuredMax == null ? Integer.MAX_VALUE : configuredMax;
        if (configuredMax != null && configuredMax <= 0) {
            maxTicketsPerPerson = DEFAULT_MAX_TICKETS_PER_PERSON;
        }
        return maxTicketsPerPerson;
    }

    private long countExistingTickets(UUID eventId, UUID requesterId, String requesterEmail) {
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
}
