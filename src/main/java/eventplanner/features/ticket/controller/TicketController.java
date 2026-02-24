package eventplanner.features.ticket.controller;

import eventplanner.features.ticket.dto.request.IssueTicketRequest;
import eventplanner.features.ticket.dto.request.BulkTicketActionRequest;
import eventplanner.features.ticket.dto.request.ResendTicketRequest;
import eventplanner.features.ticket.dto.request.UpdateTicketRequest;
import eventplanner.features.ticket.dto.request.TransferTicketRequest;
import eventplanner.features.ticket.dto.request.ValidateTicketRequest;
import eventplanner.features.ticket.dto.response.BulkTicketActionResponse;
import eventplanner.features.ticket.dto.response.TicketResponse;
import eventplanner.features.ticket.dto.response.TicketValidationResponse;
import eventplanner.features.ticket.dto.response.TicketWalletResponse;
import eventplanner.features.ticket.entity.Ticket;
import eventplanner.features.ticket.enums.TicketStatus;
import eventplanner.features.ticket.service.TicketService;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ForbiddenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for ticket management operations.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Tickets")
@RequiredArgsConstructor
public class TicketController {

    /** Maximum page size for ticket queries to prevent large result-set scraping. */
    private static final int MAX_PAGE_SIZE = 50;

    /** Whitelist of allowed sort fields — rejects arbitrary column names that could
     *  leak schema info or trigger expensive sorts on non-indexed columns. */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "status", "ownerEmail", "ownerName", "ticketNumber"
    );

    private final TicketService ticketService;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Operation(summary = "Issue tickets", 
        description = "Issue tickets to one or more attendees. Accepts a single ticket request or a list of requests. " +
                     "For free tickets, automatically sets attendee RSVP to ACCEPTED. " +
                     "Each request can issue multiple tickets (quantity) to a single attendee/email.")
    @RequiresPermission(value = RbacPermissions.TICKET_CREATE, resources = {"event_id=#requests[0].eventId"})
    public ResponseEntity<List<TicketResponse>> issueTickets(
            @Valid @RequestBody List<IssueTicketRequest> requests,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException(
                "At least one ticket request is required");
        }
        // Verify all requests are for the same event (for permission check)
        UUID eventId = requests.get(0).getEventId();
        for (IssueTicketRequest req : requests) {
            if (req.getEventId() == null || !req.getEventId().equals(eventId)) {
                throw new BadRequestException(
                    "All ticket requests must be for the same event");
            }
        }
        // Verify user can access the event
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ForbiddenException(
                "Access denied to event: " + eventId);
        }
        List<Ticket> allTickets = ticketService.issueTickets(requests, principal);
        List<TicketResponse> responses = allTickets.stream()
            .map(TicketResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get tickets for event", 
        description = "Get tickets for an event with pagination and filtering. " +
                     "Use 'ticketId' filter to get a specific ticket by ID. " +
                     "Supports filtering by status and ticketTypeId.")
    @RequiresPermission(value = RbacPermissions.TICKET_READ, resources = {"event_id=#eventId"})
    public ResponseEntity<Page<TicketResponse>> getTicketsByEvent(
            @PathVariable UUID eventId,
            @RequestParam(required = false) UUID ticketId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) UUID ticketTypeId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify user can access the event
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ForbiddenException(
                "Access denied to event: " + eventId);
        }
        // Validate pagination
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        // Whitelist sort field to prevent schema probing and unindexed-column DoS
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException(
                "Invalid sort field. Allowed: " + ALLOWED_SORT_FIELDS);
        }
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<TicketResponse> tickets = ticketService.getTicketsByEventId(
            eventId, ticketId, status, ticketTypeId, pageable);
        return ResponseEntity.ok(tickets);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate ticket", 
        description = "Validate a ticket via QR code scanning. Updates ticket status to VALIDATED and attendee check-in status.")
    @RequiresPermission(value = RbacPermissions.TICKET_VALIDATE, resources = {"event_id=#request.eventId"})
    public ResponseEntity<TicketValidationResponse> validateTicket(
            @Valid @RequestBody ValidateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify user can access the event
        if (!authorizationService.canAccessEvent(principal, request.getEventId())) {
            throw new ForbiddenException(
                "Access denied to event: " + request.getEventId());
        }
        Ticket ticket = ticketService.validateTicket(request, principal);
        TicketValidationResponse response = TicketValidationResponse.builder()
            .valid(true)
            .ticket(TicketResponse.from(ticket))
            .message("Ticket validated successfully")
            .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel ticket", 
        description = "Cancel a ticket. Cannot cancel if already validated.")
    @RequiresPermission(value = RbacPermissions.TICKET_CANCEL, resources = {"ticket_id=#id"})
    public ResponseEntity<TicketResponse> cancelTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Ticket ticket = ticketService.cancelTicket(id, principal);
        TicketResponse response;
        response = TicketResponse.from(ticket);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund ticket", description = "Refund a ticket and update inventory counts.")
    @RequiresPermission(value = RbacPermissions.TICKET_CANCEL, resources = {"ticket_id=#id"})
    public ResponseEntity<TicketResponse> refundTicket(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        Ticket ticket = ticketService.refundTicket(id, reason, principal);
        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ticket", description = "Update ticket holder details for email-based tickets.")
    @RequiresPermission(value = RbacPermissions.TICKET_CREATE, resources = {"ticket_id=#id"})
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Ticket ticket = ticketService.updateTicket(id, request, principal);
        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    @PostMapping("/{id}/transfer")
    @Operation(summary = "Transfer ticket", description = "Transfer ticket ownership to another attendee or email.")
    @RequiresPermission(value = RbacPermissions.TICKET_CREATE, resources = {"ticket_id=#id"})
    public ResponseEntity<TicketResponse> transferTicket(
            @PathVariable UUID id,
            @Valid @RequestBody TransferTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Ticket ticket = ticketService.transferTicket(id, request, principal);
        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "Resend ticket notifications", description = "Resend ticket email and/or push notification.")
    @RequiresPermission(value = RbacPermissions.TICKET_READ, resources = {"ticket_id=#id"})
    public ResponseEntity<TicketResponse> resendTicket(
            @PathVariable UUID id,
            @RequestBody(required = false) ResendTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Ticket ticket = ticketService.resendTicket(id, request, principal);
        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk ticket actions", description = "Cancel, refund, or resend tickets in bulk.")
    @RequiresPermission(value = RbacPermissions.TICKET_CANCEL, resources = {"event_id=#request.eventId"})
    public ResponseEntity<BulkTicketActionResponse> bulkAction(
            @Valid @RequestBody BulkTicketActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (request == null || request.getEventId() == null) {
            throw new BadRequestException("Event ID is required");
        }
        if (!authorizationService.canAccessEvent(principal, request.getEventId())) {
            throw new ForbiddenException("Access denied to event: " + request.getEventId());
        }
        BulkTicketActionResponse response = ticketService.bulkAction(request, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/wallet-pass")
    @Operation(summary = "Get wallet pass data",
        description = "Get wallet-ready data for adding the ticket to Apple or Google Wallet.")
    @RequiresPermission(value = RbacPermissions.TICKET_READ, resources = {"ticket_id=#id"})
    public ResponseEntity<TicketWalletResponse> getWalletPass(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketWalletResponse wallet = ticketService.getTicketWallet(id);
        return ResponseEntity.ok(wallet);
    }

}
