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
import eventplanner.features.ticket.dto.request.CreateTicketApprovalRequest;
import eventplanner.features.ticket.dto.request.IssueTicketRequest;
import eventplanner.features.ticket.dto.request.TicketApprovalDecisionRequest;
import eventplanner.features.ticket.entity.TicketApprovalRequest;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.enums.TicketApprovalStatus;
import eventplanner.features.ticket.repository.TicketApprovalRequestRepository;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
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
public class TicketApprovalService {

    private static final int DEFAULT_MAX_TICKETS_PER_PERSON = 5;

    private final TicketApprovalRequestRepository approvalRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final AttendeeRepository attendeeRepository;
    private final TicketService ticketService;
    private final AuthorizationService authorizationService;
    private final TicketingPolicyService ticketingPolicyService;

    public TicketApprovalRequest createRequest(UUID eventId, CreateTicketApprovalRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new BadRequestException("Request cannot be null");
        }
        if (principal == null || principal.getUser() == null) {
            throw new BadRequestException("Authenticated user is required");
        }

        Event event = loadEvent(eventId);
        ticketingPolicyService.ensureEventOpenForTicketing(event);

        TicketType ticketType = loadTicketType(event, request.getTicketTypeId());
        ensureTicketTypeOnSale(ticketType);
        ensureApprovalRequired(ticketType);

        if (!ticketType.canPurchase(request.getQuantity())) {
            throw new ApiException(ErrorCode.TICKET_TYPE_SOLD_OUT, "Not enough tickets available");
        }

        UUID requesterId = principal.getId();
        if (approvalRepository.existsByEventIdAndTicketTypeIdAndRequesterIdAndStatus(
                eventId, ticketType.getId(), requesterId, TicketApprovalStatus.PENDING)) {
            throw new ConflictException("You already have a pending approval request for this ticket type");
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

        TicketApprovalRequest approvalRequest = new TicketApprovalRequest();
        approvalRequest.setEvent(event);
        approvalRequest.setTicketType(ticketType);
        approvalRequest.setRequester(principal.getUser());
        approvalRequest.setRequesterEmail(requesterEmail);
        approvalRequest.setRequesterName(requesterName);
        approvalRequest.setQuantity(request.getQuantity());

        return approvalRepository.save(approvalRequest);
    }

    @Transactional(readOnly = true)
    public Page<TicketApprovalRequest> listRequests(UUID eventId, TicketApprovalStatus status, Pageable pageable) {
        if (status == null) {
            return approvalRepository.findByEventId(eventId, pageable);
        }
        return approvalRepository.findByEventIdAndStatus(eventId, status, pageable);
    }

    @Transactional(readOnly = true)
    public List<TicketApprovalRequest> listRequestsForUser(UUID eventId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Authenticated user is required");
        }
        return approvalRepository.findByRequesterIdAndEventId(principal.getId(), eventId);
    }

    public TicketApprovalRequest approveRequest(UUID eventId, UUID requestId, TicketApprovalDecisionRequest decision,
                                                UserPrincipal principal) {
        ensureCanManageRequests(principal, eventId);
        TicketApprovalRequest request = loadRequest(eventId, requestId);

        if (request.getStatus() != TicketApprovalStatus.PENDING) {
            throw new ConflictException("Approval request is not pending");
        }

        Event event = request.getEvent() != null ? request.getEvent() : loadEvent(eventId);
        ticketingPolicyService.ensureEventOpenForTicketing(event);

        TicketType ticketType = request.getTicketType();
        if (ticketType == null) {
            throw new BadRequestException("Approval request is missing ticket type");
        }
        ensureTicketTypeOnSale(ticketType);

        IssueTicketRequest issueRequest = buildIssueRequest(event, request, decision);
        ticketService.issueTickets(List.of(issueRequest), principal, true, true);

        request.approve(principal.getUser());
        if (decision != null && decision.getNote() != null) {
            request.setDecisionNote(decision.getNote());
        }
        return approvalRepository.save(request);
    }

    public TicketApprovalRequest rejectRequest(UUID eventId, UUID requestId, TicketApprovalDecisionRequest decision,
                                               UserPrincipal principal) {
        ensureCanManageRequests(principal, eventId);
        TicketApprovalRequest request = loadRequest(eventId, requestId);
        String note = decision != null ? decision.getNote() : null;
        try {
            request.reject(principal.getUser(), note);
        } catch (IllegalStateException e) {
            throw new ConflictException(e.getMessage());
        }
        return approvalRepository.save(request);
    }

    public TicketApprovalRequest cancelRequest(UUID eventId, UUID requestId, UserPrincipal principal) {
        TicketApprovalRequest request = loadRequest(eventId, requestId);
        if (!canModifyRequest(principal, request)) {
            throw new ForbiddenException("Access denied to approval request");
        }
        try {
            request.cancel();
        } catch (IllegalStateException e) {
            throw new ConflictException(e.getMessage());
        }
        return approvalRepository.save(request);
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

    private TicketApprovalRequest loadRequest(UUID eventId, UUID requestId) {
        return approvalRepository.findByIdAndEventId(requestId, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + requestId));
    }

    private void ensureTicketTypeOnSale(TicketType ticketType) {
        if (!ticketType.isOnSale()) {
            throw new BadRequestException("Ticket sales are not active for this ticket type");
        }
    }

    private void ensureApprovalRequired(TicketType ticketType) {
        if (!Boolean.TRUE.equals(ticketType.getRequiresApproval())) {
            throw new BadRequestException("Ticket type does not require approval");
        }
    }

    private void ensureCanManageRequests(UserPrincipal principal, UUID eventId) {
        if (principal == null || eventId == null) {
            throw new ForbiddenException("Access denied to approval requests");
        }
        if (authorizationService.isAdmin(principal) || authorizationService.isEventOwner(principal, eventId)) {
            return;
        }
        if (!authorizationService.hasEventMembership(principal, eventId)) {
            throw new ForbiddenException("Access denied to approval requests");
        }
    }

    private boolean canModifyRequest(UserPrincipal principal, TicketApprovalRequest request) {
        if (principal == null || request == null || request.getEvent() == null) {
            return false;
        }
        UUID requesterId = request.getRequester() != null ? request.getRequester().getId() : null;
        if (requesterId != null && requesterId.equals(principal.getId())) {
            return true;
        }
        UUID eventId = request.getEvent().getId();
        return authorizationService.isAdmin(principal) ||
            authorizationService.isEventOwner(principal, eventId) ||
            authorizationService.hasEventMembership(principal, eventId);
    }

    private IssueTicketRequest buildIssueRequest(Event event, TicketApprovalRequest request,
                                                 TicketApprovalDecisionRequest decision) {
        UserAccount requester = request.getRequester();
        String requesterEmail = normalizeEmail(request.getRequesterEmail());
        String requesterName = request.getRequesterName();
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
            attendee = attendeeRepository.findByEventIdAndUserId(event.getId(), requester.getId()).orElse(null);
        }
        if (attendee == null && requesterEmail != null) {
            attendee = attendeeRepository.findByEventIdAndEmailIgnoreCase(event.getId(), requesterEmail).orElse(null);
        }

        IssueTicketRequest issueRequest = new IssueTicketRequest();
        issueRequest.setEventId(event.getId());
        issueRequest.setTicketTypeId(request.getTicketType().getId());
        issueRequest.setQuantity(request.getQuantity());
        if (attendee != null) {
            issueRequest.setAttendeeId(attendee.getId());
        } else {
            issueRequest.setOwnerEmail(requesterEmail);
            issueRequest.setOwnerName(requesterName);
        }
        boolean sendEmail = decision == null || Boolean.TRUE.equals(decision.getSendEmail());
        boolean sendPush = decision == null || Boolean.TRUE.equals(decision.getSendPush());
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
