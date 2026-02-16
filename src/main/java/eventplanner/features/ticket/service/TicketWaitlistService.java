package eventplanner.features.ticket.service;

import eventplanner.common.exception.exceptions.ApiException;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ConflictException;
import eventplanner.common.exception.exceptions.ErrorCode;
import eventplanner.common.exception.exceptions.ForbiddenException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.features.event.entity.Event;
import eventplanner.features.ticket.dto.request.CreateTicketWaitlistRequest;
import eventplanner.features.ticket.dto.request.IssueTicketRequest;
import eventplanner.features.ticket.dto.request.TicketWaitlistFulfillRequest;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.entity.TicketWaitlistEntry;
import eventplanner.features.ticket.enums.TicketWaitlistStatus;
import eventplanner.features.ticket.repository.TicketWaitlistEntryRepository;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketWaitlistService {

    private final TicketWaitlistEntryRepository waitlistRepository;
    private final TicketService ticketService;
    private final AuthorizationService authorizationService;
    private final TicketingPolicyService ticketingPolicyService;
    private final TicketPolicyHelper policyHelper;

    public TicketWaitlistEntry createEntry(UUID eventId, CreateTicketWaitlistRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new BadRequestException("Request cannot be null");
        }
        if (principal == null || principal.getUser() == null) {
            throw new BadRequestException("Authenticated user is required");
        }

        Event event = policyHelper.loadEvent(eventId);
        ticketingPolicyService.ensureEventOpenForTicketing(event);

        TicketType ticketType = policyHelper.loadTicketType(event, request.getTicketTypeId());
        policyHelper.ensureTicketTypeOnSale(ticketType);

        if (ticketType.canPurchase(request.getQuantity())) {
            throw new ConflictException("Tickets are available; purchase instead of joining the waitlist");
        }

        UUID requesterId = principal.getId();
        if (waitlistRepository.existsByEventIdAndTicketTypeIdAndRequesterIdAndStatus(
                eventId, ticketType.getId(), requesterId, TicketWaitlistStatus.WAITING)) {
            throw new ConflictException("You are already on the waitlist for this ticket type");
        }

        String requesterEmail = TicketPolicyHelper.normalizeEmail(principal.getUser().getEmail());
        String requesterName = principal.getUser().getName() != null
            ? principal.getUser().getName().trim()
            : requesterEmail;
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new BadRequestException("Requester email is required");
        }

        policyHelper.enforceMaxTicketsPerPerson(ticketType, eventId, requesterId, requesterEmail, request.getQuantity());

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

    private TicketWaitlistEntry loadEntry(UUID eventId, UUID entryId) {
        return waitlistRepository.findByIdAndEventId(entryId, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found: " + entryId));
    }

    private void ensureCanManageWaitlist(UserPrincipal principal, UUID eventId) {
        if (principal == null || eventId == null) {
            throw new ForbiddenException("Access denied to waitlist");
        }
        if (!authorizationService.canManageEvent(principal, eventId)) {
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
        return authorizationService.canManageEvent(principal, entry.getEvent().getId());
    }

    private IssueTicketRequest buildIssueRequest(TicketWaitlistEntry entry, TicketWaitlistFulfillRequest request) {
        boolean sendEmail = request == null || Boolean.TRUE.equals(request.getSendEmail());
        boolean sendPush = request == null || Boolean.TRUE.equals(request.getSendPush());
        return policyHelper.buildIssueRequest(
            entry.getEvent().getId(), entry.getTicketType().getId(), entry.getQuantity(),
            entry.getRequester(), entry.getRequesterEmail(), entry.getRequesterName(),
            sendEmail, sendPush);
    }
}
