package eventplanner.features.ticket.service;

import eventplanner.common.exception.exceptions.ApiException;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ConflictException;
import eventplanner.common.exception.exceptions.ErrorCode;
import eventplanner.common.exception.exceptions.ForbiddenException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.features.event.entity.Event;
import eventplanner.features.ticket.dto.request.CreateTicketApprovalRequest;
import eventplanner.features.ticket.dto.request.IssueTicketRequest;
import eventplanner.features.ticket.dto.request.TicketApprovalDecisionRequest;
import eventplanner.features.ticket.entity.TicketApprovalRequest;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.enums.TicketApprovalStatus;
import eventplanner.features.ticket.repository.TicketApprovalRequestRepository;
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
public class TicketApprovalService {

    private final TicketApprovalRequestRepository approvalRepository;
    private final TicketService ticketService;
    private final AuthorizationService authorizationService;
    private final TicketingPolicyService ticketingPolicyService;
    private final TicketPolicyHelper policyHelper;

    public TicketApprovalRequest createRequest(UUID eventId, CreateTicketApprovalRequest request, UserPrincipal principal) {
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
        ensureApprovalRequired(ticketType);

        if (!ticketType.canPurchase(request.getQuantity())) {
            throw new ApiException(ErrorCode.TICKET_TYPE_SOLD_OUT, "Not enough tickets available");
        }

        UUID requesterId = principal.getId();
        if (approvalRepository.existsByEventIdAndTicketTypeIdAndRequesterIdAndStatus(
                eventId, ticketType.getId(), requesterId, TicketApprovalStatus.PENDING)) {
            throw new ConflictException("You already have a pending approval request for this ticket type");
        }

        String requesterEmail = TicketPolicyHelper.normalizeEmail(principal.getUser().getEmail());
        String requesterName = principal.getUser().getName() != null
            ? principal.getUser().getName().trim()
            : requesterEmail;
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new BadRequestException("Requester email is required");
        }

        policyHelper.enforceMaxTicketsPerPerson(ticketType, eventId, requesterId, requesterEmail, request.getQuantity());

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

        Event event = request.getEvent() != null ? request.getEvent() : policyHelper.loadEvent(eventId);
        ticketingPolicyService.ensureEventOpenForTicketing(event);

        TicketType ticketType = request.getTicketType();
        if (ticketType == null) {
            throw new BadRequestException("Approval request is missing ticket type");
        }
        policyHelper.ensureTicketTypeOnSale(ticketType);

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

    private TicketApprovalRequest loadRequest(UUID eventId, UUID requestId) {
        return approvalRepository.findByIdAndEventId(requestId, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + requestId));
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
        if (!authorizationService.canManageEvent(principal, eventId)) {
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
        return authorizationService.canManageEvent(principal, request.getEvent().getId());
    }

    private IssueTicketRequest buildIssueRequest(Event event, TicketApprovalRequest request,
                                                 TicketApprovalDecisionRequest decision) {
        boolean sendEmail = decision == null || Boolean.TRUE.equals(decision.getSendEmail());
        boolean sendPush = decision == null || Boolean.TRUE.equals(decision.getSendPush());
        return policyHelper.buildIssueRequest(
            event.getId(), request.getTicketType().getId(), request.getQuantity(),
            request.getRequester(), request.getRequesterEmail(), request.getRequesterName(),
            sendEmail, sendPush);
    }
}
