package eventplanner.features.ticket.controller;

import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ForbiddenException;
import eventplanner.features.ticket.dto.request.CreateTicketApprovalRequest;
import eventplanner.features.ticket.dto.request.CreateTicketWaitlistRequest;
import eventplanner.features.ticket.dto.request.TicketApprovalDecisionRequest;
import eventplanner.features.ticket.dto.request.TicketWaitlistFulfillRequest;
import eventplanner.features.ticket.dto.response.TicketApprovalRequestResponse;
import eventplanner.features.ticket.dto.response.TicketWaitlistEntryResponse;
import eventplanner.features.ticket.enums.TicketApprovalStatus;
import eventplanner.features.ticket.enums.TicketWaitlistStatus;
import eventplanner.features.ticket.service.TicketApprovalService;
import eventplanner.features.ticket.service.TicketWaitlistService;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/tickets")
@Tag(name = "Ticket Requests")
@RequiredArgsConstructor
public class TicketRequestController {

    private final TicketApprovalService approvalService;
    private final TicketWaitlistService waitlistService;
    private final AuthorizationService authorizationService;

    // ==================== Approval Requests ====================

    @PostMapping("/requests")
    @Operation(summary = "Request tickets for approval", description = "Create a ticket approval request when approval is required.")
    @RequiresPermission(value = RbacPermissions.TICKET_CHECKOUT, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketApprovalRequestResponse> requestApproval(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateTicketApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccessTicketing(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(TicketApprovalRequestResponse.from(
                approvalService.createRequest(eventId, request, principal)));
    }

    @GetMapping("/requests")
    @Operation(summary = "List ticket approval requests", description = "List approval requests for an event.")
    @RequiresPermission(value = RbacPermissions.TICKET_READ, resources = {"event_id=#eventId"})
    public ResponseEntity<Page<TicketApprovalRequestResponse>> listApprovalRequests(
            @PathVariable UUID eventId,
            @RequestParam(required = false) TicketApprovalStatus status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
            Sort.by(direction, sortBy));
        Page<TicketApprovalRequestResponse> responses = approvalService
            .listRequests(eventId, status, pageable)
            .map(TicketApprovalRequestResponse::from);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/requests/mine")
    @Operation(summary = "List my ticket approval requests", description = "List the current user's approval requests for an event.")
    @RequiresPermission(value = RbacPermissions.TICKET_CHECKOUT, resources = {"event_id=#eventId"})
    public ResponseEntity<List<TicketApprovalRequestResponse>> listMyApprovalRequests(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccessTicketing(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
        List<TicketApprovalRequestResponse> responses = approvalService
            .listRequestsForUser(eventId, principal).stream()
            .map(TicketApprovalRequestResponse::from)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/requests/{requestId}/approve")
    @Operation(summary = "Approve ticket request", description = "Approve a ticket request and issue tickets.")
    @RequiresPermission(value = RbacPermissions.TICKET_CREATE, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketApprovalRequestResponse> approveRequest(
            @PathVariable UUID eventId,
            @PathVariable UUID requestId,
            @RequestBody(required = false) TicketApprovalDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketApprovalRequestResponse response = TicketApprovalRequestResponse.from(
            approvalService.approveRequest(eventId, requestId, request, principal));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{requestId}/reject")
    @Operation(summary = "Reject ticket request", description = "Reject a ticket approval request.")
    @RequiresPermission(value = RbacPermissions.TICKET_CREATE, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketApprovalRequestResponse> rejectRequest(
            @PathVariable UUID eventId,
            @PathVariable UUID requestId,
            @RequestBody(required = false) TicketApprovalDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketApprovalRequestResponse response = TicketApprovalRequestResponse.from(
            approvalService.rejectRequest(eventId, requestId, request, principal));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/requests/{requestId}")
    @Operation(summary = "Cancel ticket request", description = "Cancel a pending ticket approval request.")
    @RequiresPermission(value = RbacPermissions.TICKET_CHECKOUT, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketApprovalRequestResponse> cancelRequest(
            @PathVariable UUID eventId,
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketApprovalRequestResponse response = TicketApprovalRequestResponse.from(
            approvalService.cancelRequest(eventId, requestId, principal));
        return ResponseEntity.ok(response);
    }

    // ==================== Waitlist ====================

    @PostMapping("/waitlist")
    @Operation(summary = "Join ticket waitlist", description = "Join the waitlist when tickets are sold out.")
    @RequiresPermission(value = RbacPermissions.TICKET_CHECKOUT, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketWaitlistEntryResponse> joinWaitlist(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateTicketWaitlistRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccessTicketing(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
        TicketWaitlistEntryResponse response = TicketWaitlistEntryResponse.from(
            waitlistService.createEntry(eventId, request, principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/waitlist")
    @Operation(summary = "List waitlist entries", description = "List waitlist entries for an event.")
    @RequiresPermission(value = RbacPermissions.TICKET_READ, resources = {"event_id=#eventId"})
    public ResponseEntity<Page<TicketWaitlistEntryResponse>> listWaitlist(
            @PathVariable UUID eventId,
            @RequestParam(required = false) TicketWaitlistStatus status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
            Sort.by(direction, sortBy));
        Page<TicketWaitlistEntryResponse> responses = waitlistService
            .listEntries(eventId, status, pageable)
            .map(TicketWaitlistEntryResponse::from);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/waitlist/mine")
    @Operation(summary = "List my waitlist entries", description = "List the current user's waitlist entries for an event.")
    @RequiresPermission(value = RbacPermissions.TICKET_CHECKOUT, resources = {"event_id=#eventId"})
    public ResponseEntity<List<TicketWaitlistEntryResponse>> listMyWaitlist(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccessTicketing(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
        List<TicketWaitlistEntryResponse> responses = waitlistService
            .listEntriesForUser(eventId, principal).stream()
            .map(TicketWaitlistEntryResponse::from)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/waitlist/{entryId}/fulfill")
    @Operation(summary = "Fulfill waitlist entry", description = "Issue tickets for a waitlist entry.")
    @RequiresPermission(value = RbacPermissions.TICKET_CREATE, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketWaitlistEntryResponse> fulfillWaitlist(
            @PathVariable UUID eventId,
            @PathVariable UUID entryId,
            @RequestBody(required = false) TicketWaitlistFulfillRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketWaitlistEntryResponse response = TicketWaitlistEntryResponse.from(
            waitlistService.fulfillEntry(eventId, entryId, request, principal));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/waitlist/{entryId}")
    @Operation(summary = "Cancel waitlist entry", description = "Cancel a waitlist entry.")
    @RequiresPermission(value = RbacPermissions.TICKET_CHECKOUT, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketWaitlistEntryResponse> cancelWaitlist(
            @PathVariable UUID eventId,
            @PathVariable UUID entryId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketWaitlistEntryResponse response = TicketWaitlistEntryResponse.from(
            waitlistService.cancelEntry(eventId, entryId, principal));
        return ResponseEntity.ok(response);
    }

    private boolean canAccessTicketing(UserPrincipal principal, UUID eventId) {
        return authorizationService.canAccessEventWithInvite(principal, eventId);
    }
}
