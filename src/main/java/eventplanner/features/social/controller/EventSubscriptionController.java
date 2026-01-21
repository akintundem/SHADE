package eventplanner.features.social.controller;

import eventplanner.features.social.dto.request.EventSubscriptionRequest;
import eventplanner.features.social.dto.response.EventSubscriptionResponse;
import eventplanner.features.social.dto.response.UserProfileResponse;
import eventplanner.features.social.service.EventSubscriptionService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Subscriptions", description = "Subscribe to events to receive updates in timeline")
@SecurityRequirement(name = "bearerAuth")
public class EventSubscriptionController {

    private final EventSubscriptionService subscriptionService;

    public EventSubscriptionController(EventSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/{eventId}/subscribe")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Subscribe to event", description = "Subscribe to an event to receive updates")
    public ResponseEntity<EventSubscriptionResponse> subscribeToEvent(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) EventSubscriptionRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.subscribeToEvent(eventId, principal, request));
    }

    @DeleteMapping("/{eventId}/subscribe")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Unsubscribe from event", description = "Unsubscribe from event updates")
    public ResponseEntity<Void> unsubscribeFromEvent(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        subscriptionService.unsubscribeFromEvent(eventId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/subscribers")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get event subscribers", description = "Get list of users subscribed to this event")
    public ResponseEntity<Page<UserProfileResponse>> getEventSubscribers(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(subscriptionService.getEventSubscribers(eventId, pageable));
    }

    @GetMapping("/{eventId}/subscriber-count")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get subscriber count", description = "Get number of subscribers for this event")
    public ResponseEntity<Long> getSubscriberCount(
            @Parameter(description = "Event ID") @PathVariable UUID eventId
    ) {
        return ResponseEntity.ok(subscriptionService.getSubscriberCount(eventId));
    }

    @GetMapping("/{eventId}/is-subscribed")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Check subscription status", description = "Check if current user is subscribed to this event")
    public ResponseEntity<Boolean> isSubscribed(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(subscriptionService.isSubscribed(eventId, principal));
    }
}
