package eventplanner.features.event.controller;

import eventplanner.features.event.dto.request.CreateEventWaitlistRequest;
import eventplanner.features.event.dto.response.EventWaitlistEntryResponse;
import eventplanner.features.event.entity.EventWaitlistEntry;
import eventplanner.features.event.enums.EventWaitlistStatus;
import eventplanner.features.event.service.EventWaitlistService;
import eventplanner.common.util.Preconditions;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing event waitlist entries.
 */
@RestController
@RequestMapping("/api/v1/events/{eventId}/waitlist")
@Tag(name = "Event Waitlist", description = "Manage event waitlist when capacity is reached")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class EventWaitlistController {

    private final EventWaitlistService waitlistService;

    @PostMapping
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Join event waitlist", 
            description = "Join the waitlist for an event when it's at capacity. " +
                         "You will be automatically promoted when spots become available.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully joined waitlist",
                content = @Content(schema = @Schema(implementation = EventWaitlistEntryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "409", description = "Already on waitlist or event not at capacity")
    })
    public ResponseEntity<EventWaitlistEntryResponse> joinWaitlist(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) CreateEventWaitlistRequest request) {
        CreateEventWaitlistRequest req = request != null ? request : new CreateEventWaitlistRequest();
        EventWaitlistEntry entry = waitlistService.createEntry(eventId, req, principal);
        EventWaitlistEntryResponse response = EventWaitlistEntryResponse.from(entry);
        URI location = URI.create("/api/v1/events/" + eventId + "/waitlist/" + entry.getId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @RequiresPermission(RbacPermissions.ATTENDEE_READ)
    @Operation(summary = "List waitlist entries", 
            description = "List waitlist entries for an event. Event managers can see all entries.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Waitlist entries retrieved successfully")
    })
    public ResponseEntity<Page<EventWaitlistEntryResponse>> listWaitlist(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Filter by status") @RequestParam(required = false) EventWaitlistStatus status,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventWaitlistEntry> entries = waitlistService.listEntries(eventId, status, pageable, principal);
        Page<EventWaitlistEntryResponse> responses = entries.map(EventWaitlistEntryResponse::from);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my-entries")
    @RequiresPermission(RbacPermissions.ATTENDEE_READ)
    @Operation(summary = "Get my waitlist entries", 
            description = "Get waitlist entries for the current user for this event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Waitlist entries retrieved successfully")
    })
    public ResponseEntity<List<EventWaitlistEntryResponse>> getMyEntries(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Preconditions.requireAuthenticated(principal);
        List<EventWaitlistEntry> entries = waitlistService.listEntriesForUser(eventId, principal);
        List<EventWaitlistEntryResponse> responses = entries.stream()
            .map(EventWaitlistEntryResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{entryId}")
    @RequiresPermission(RbacPermissions.ATTENDEE_DELETE)
    @Operation(summary = "Cancel waitlist entry", 
            description = "Cancel a waitlist entry. Users can cancel their own entries, " +
                         "event managers can cancel any entry.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Waitlist entry cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Waitlist entry not found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Entry cannot be cancelled (already promoted/cancelled)")
    })
    public ResponseEntity<Void> cancelEntry(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            @Parameter(description = "Waitlist entry ID") @PathVariable UUID entryId,
            @AuthenticationPrincipal UserPrincipal principal) {
        waitlistService.cancelEntry(eventId, entryId, principal);
        return ResponseEntity.noContent().build();
    }
}
