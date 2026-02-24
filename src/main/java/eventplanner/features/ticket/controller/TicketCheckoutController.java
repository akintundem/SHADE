package eventplanner.features.ticket.controller;

import eventplanner.features.ticket.dto.request.CreateTicketCheckoutRequest;
import eventplanner.features.ticket.dto.response.TicketCheckoutResponse;
import eventplanner.features.ticket.dto.response.TicketPaymentInitResponse;
import eventplanner.features.ticket.dto.response.TicketFakePaymentResponse;
import eventplanner.features.ticket.service.TicketCheckoutService;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.exception.exceptions.ForbiddenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Checkout endpoints for ticket purchases (pre-payment).
 */
@RestController
@RequestMapping("/api/v1/events/{eventId}/tickets/checkout")
@Tag(name = "Ticket Checkout")
@RequiredArgsConstructor
public class TicketCheckoutController {

    private final TicketCheckoutService checkoutService;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Operation(summary = "Start ticket checkout", description = "Create a checkout session, reserve tickets, and return a cost breakdown.")
    @RequiresPermission(value = RbacPermissions.TICKET_CHECKOUT, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketCheckoutResponse> createCheckout(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateTicketCheckoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccessCheckout(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
        TicketCheckoutResponse response = checkoutService.createCheckout(eventId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{checkoutId}/start-payment")
    @Operation(summary = "Start payment session", description = "Initiate payment for a checkout (placeholder until PSP is wired).")
    @RequiresPermission(value = RbacPermissions.TICKET_CHECKOUT, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketPaymentInitResponse> startPayment(
            @PathVariable UUID eventId,
            @PathVariable UUID checkoutId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccessCheckout(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
        TicketPaymentInitResponse response = checkoutService.startPayment(checkoutId, eventId, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{checkoutId}/fake-payment")
    @Operation(summary = "Complete fake payment (demo)", description = "Simulate payment outcome: ~90% success, ~10% failure. On success, completes checkout and issues tickets.")
    @RequiresPermission(value = RbacPermissions.TICKET_CHECKOUT, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketFakePaymentResponse> completeFakePayment(
            @PathVariable UUID eventId,
            @PathVariable UUID checkoutId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccessCheckout(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
        TicketFakePaymentResponse response = checkoutService.completeFakePayment(checkoutId, eventId, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{checkoutId}")
    @Operation(summary = "Get checkout session", description = "Retrieve checkout details and cost breakdown. Automatically expires if the hold window passed.")
    @RequiresPermission(value = RbacPermissions.TICKET_CHECKOUT, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketCheckoutResponse> getCheckout(
            @PathVariable UUID eventId,
            @PathVariable UUID checkoutId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccessCheckout(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
        TicketCheckoutResponse response = checkoutService.getCheckout(checkoutId, eventId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{checkoutId}/cancel")
    @Operation(summary = "Cancel checkout", description = "Cancel a checkout session and release reserved inventory.")
    @RequiresPermission(value = RbacPermissions.TICKET_CHECKOUT, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketCheckoutResponse> cancelCheckout(
            @PathVariable UUID eventId,
            @PathVariable UUID checkoutId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccessCheckout(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
        TicketCheckoutResponse response = checkoutService.cancelCheckout(checkoutId, eventId);
        return ResponseEntity.ok(response);
    }

    private boolean canAccessCheckout(UserPrincipal principal, UUID eventId) {
        return authorizationService.canAccessEventWithInvite(principal, eventId);
    }
}
