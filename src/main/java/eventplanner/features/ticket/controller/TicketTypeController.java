package eventplanner.features.ticket.controller;

import eventplanner.features.ticket.dto.request.CreateTicketTypeRequest;
import eventplanner.features.ticket.dto.request.CloneTicketTypeRequest;
import eventplanner.features.ticket.dto.request.UpdateTicketTypeRequest;
import eventplanner.features.ticket.dto.response.TicketTypeResponse;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.enums.TicketTypeCategory;
import eventplanner.features.ticket.service.TicketTypeService;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing ticket types within an event.
 * Allows event owners to create, update, and delete ticket types.
 */
@RestController
@RequestMapping("/api/v1/events/{eventId}/ticket-types")
@Tag(name = "Ticket Types", description = "Ticket type management for events")
@RequiredArgsConstructor
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;
    private final AuthorizationService authorizationService;

    /**
     * Creates a new ticket type for a specific event.
     */
    @PostMapping
    @Operation(summary = "Create ticket type", description = "Create a new ticket type for an event.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ticket type created successfully",
            content = @Content(schema = @Schema(implementation = TicketTypeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @RequiresPermission(value = RbacPermissions.TICKET_TYPE_CREATE, resources = {"event_id=#eventId"})
    public ResponseEntity<TicketTypeResponse> createTicketType(
        @Parameter(description = "ID of the event") @PathVariable UUID eventId,
        @Valid @RequestBody CreateTicketTypeRequest request,
        @AuthenticationPrincipal UserPrincipal principal) {
        // Verify user can access the event
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied to event: " + eventId);
        }

        // Ensure eventId in request matches path variable
        TicketType newTicketType = ticketTypeService.createTicketType(eventId, request, principal);
        TicketTypeResponse response = ticketTypeService.getTicketTypes(eventId, newTicketType.getId(), null, null, null, principal)
            .stream().findFirst().orElse(TicketTypeResponse.from(newTicketType));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieves ticket types for an event with optional filters.
     * Supports filtering by ID, category, active status, and name.
     * Returns a list (single item if ID filter is provided).
     */
    @GetMapping
    @Operation(summary = "Get ticket types", 
        description = "Retrieve ticket types for an event. Supports filtering by ID, category, active status, and name. " +
                     "Returns a list of ticket types (single item if ID is provided).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved ticket types",
            content = @Content(schema = @Schema(implementation = TicketTypeResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket type or event not found (when ID filter is used)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @RequiresPermission(value = RbacPermissions.TICKET_TYPE_READ, resources = {"event_id=#eventId"})
    public ResponseEntity<List<TicketTypeResponse>> getTicketTypes(
        @Parameter(description = "ID of the event") @PathVariable UUID eventId,
        @Parameter(description = "Filter by specific ticket type ID") @RequestParam(required = false) UUID id,
        @Parameter(description = "Filter by category (VIP, GENERAL_ADMISSION, etc.)") @RequestParam(required = false) TicketTypeCategory category,
        @Parameter(description = "Filter to only active ticket types") @RequestParam(required = false) Boolean activeOnly,
        @Parameter(description = "Filter by name (partial match, case-insensitive)") @RequestParam(required = false) String name,
        @AuthenticationPrincipal UserPrincipal principal) {
        // Verify user can access the event
        if (!authorizationService.canAccessEventWithInvite(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "Access denied to event: " + eventId);
        }
        List<TicketTypeResponse> ticketTypes = ticketTypeService.getTicketTypes(eventId, id, category, activeOnly, name, principal);
        return ResponseEntity.ok(ticketTypes);
    }

    /**
     * Updates an existing ticket type.
     */
    @PutMapping("/{ticketTypeId}")
    @Operation(summary = "Update ticket type", description = "Update an existing ticket type for an event. Supports optimistic locking via If-Match header.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ticket type updated successfully",
            content = @Content(schema = @Schema(implementation = TicketTypeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Ticket type or event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "409", description = "Conflict - Ticket type version mismatch (optimistic locking)")
    })
    @RequiresPermission(value = RbacPermissions.TICKET_TYPE_UPDATE, resources = {"event_id=#eventId", "ticket_type_id=#ticketTypeId"})
    public ResponseEntity<TicketTypeResponse> updateTicketType(
        @Parameter(description = "ID of the event") @PathVariable UUID eventId,
        @Parameter(description = "ID of the ticket type") @PathVariable UUID ticketTypeId,
        @Valid @RequestBody UpdateTicketTypeRequest request,
        @RequestHeader(value = "If-Match", required = false) Long ifMatch, // For optimistic locking
        @AuthenticationPrincipal UserPrincipal principal) {
        // Verify user can access the event
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "Access denied to event: " + eventId);
        }
        TicketType updatedTicketType = ticketTypeService.updateTicketType(ticketTypeId, eventId, request, ifMatch, principal);
        TicketTypeResponse response = ticketTypeService.getTicketTypes(eventId, updatedTicketType.getId(), null, null, null, principal)
            .stream().findFirst().orElse(TicketTypeResponse.from(updatedTicketType));
        return ResponseEntity.ok()
            .eTag(String.valueOf(updatedTicketType.getVersion())) // Return ETag with current version
            .body(response);
    }

    /**
     * Clone a ticket type.
     */
    @PostMapping("/{ticketTypeId}/clone")
    @Operation(summary = "Clone ticket type", description = "Clone an existing ticket type within the same event.")
    @RequiresPermission(value = RbacPermissions.TICKET_TYPE_CREATE, resources = {"event_id=#eventId", "ticket_type_id=#ticketTypeId"})
    public ResponseEntity<TicketTypeResponse> cloneTicketType(
        @PathVariable UUID eventId,
        @PathVariable UUID ticketTypeId,
        @RequestBody(required = false) CloneTicketTypeRequest request,
        @AuthenticationPrincipal UserPrincipal principal) {
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied to event: " + eventId);
        }
        TicketType cloned = ticketTypeService.cloneTicketType(ticketTypeId, eventId, request, principal);
        TicketTypeResponse response = ticketTypeService.getTicketTypes(eventId, cloned.getId(), null, null, null, principal)
            .stream().findFirst().orElse(TicketTypeResponse.from(cloned));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
            /**
             * Archive a ticket type (mark inactive).
             */
            @PostMapping("/{ticketTypeId}/archive")
            @Operation(summary = "Archive ticket type", description = "Archive a ticket type (mark inactive).")
            @RequiresPermission(value = RbacPermissions.TICKET_TYPE_UPDATE, resources = {"event_id=#eventId", "ticket_type_id=#ticketTypeId"})
            public ResponseEntity<TicketTypeResponse> archiveTicketType(
                @PathVariable UUID eventId,
                @PathVariable UUID ticketTypeId,
                @AuthenticationPrincipal UserPrincipal principal) {            if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied to event: " + eventId);
        }
        TicketType archived = ticketTypeService.archiveTicketType(ticketTypeId, eventId, principal);
        TicketTypeResponse response = ticketTypeService.getTicketTypes(eventId, archived.getId(), null, null, null, principal)
            .stream().findFirst().orElse(TicketTypeResponse.from(archived));
        return ResponseEntity.ok(response);
            }
            /**
             * Restore a ticket type (mark active).
             */
            @PostMapping("/{ticketTypeId}/restore")
            @Operation(summary = "Restore ticket type", description = "Restore a ticket type (mark active).")
            @RequiresPermission(value = RbacPermissions.TICKET_TYPE_UPDATE, resources = {"event_id=#eventId", "ticket_type_id=#ticketTypeId"})
            public ResponseEntity<TicketTypeResponse> restoreTicketType(
                @PathVariable UUID eventId,
                @PathVariable UUID ticketTypeId,
                @AuthenticationPrincipal UserPrincipal principal) {            if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied to event: " + eventId);
        }
        TicketType restored = ticketTypeService.restoreTicketType(ticketTypeId, eventId, principal);
        TicketTypeResponse response = ticketTypeService.getTicketTypes(eventId, restored.getId(), null, null, null, principal)
            .stream().findFirst().orElse(TicketTypeResponse.from(restored));
        return ResponseEntity.ok(response);
            }
            /**
             * Deletes a ticket type.
             */
            @DeleteMapping("/{ticketTypeId}")
            @Operation(summary = "Delete ticket type", description = "Delete a ticket type from an event (soft delete).")
            @ApiResponses(value = {
                @ApiResponse(responseCode = "204", description = "Ticket type deleted successfully"),
                @ApiResponse(responseCode = "404", description = "Ticket type or event not found"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden")
            })
            @ResponseStatus(HttpStatus.NO_CONTENT)
            @RequiresPermission(value = RbacPermissions.TICKET_TYPE_DELETE, resources = {"event_id=#eventId", "ticket_type_id=#ticketTypeId"})
            public ResponseEntity<Void> deleteTicketType(
                @Parameter(description = "ID of the event") @PathVariable UUID eventId,
                @Parameter(description = "ID of the ticket type") @PathVariable UUID ticketTypeId,
                @AuthenticationPrincipal UserPrincipal principal) {            // Verify user can access the event
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "Access denied to event: " + eventId);
        }
        ticketTypeService.deleteTicketType(ticketTypeId, eventId, principal);
        return ResponseEntity.noContent().build();
    }

    /**
     * Hard delete a ticket type (permanent).
     */
    @DeleteMapping("/{ticketTypeId}/hard-delete")
    @Operation(summary = "Hard delete ticket type", description = "Permanently delete a ticket type (no issued tickets allowed).")
    @RequiresPermission(value = RbacPermissions.TICKET_TYPE_DELETE, resources = {"event_id=#eventId", "ticket_type_id=#ticketTypeId"})
    public ResponseEntity<Void> hardDeleteTicketType(
        @PathVariable UUID eventId,
        @PathVariable UUID ticketTypeId,
        @AuthenticationPrincipal UserPrincipal principal) {
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied to event: " + eventId);
        }
        ticketTypeService.hardDeleteTicketType(ticketTypeId, eventId, principal);
        return ResponseEntity.noContent().build();
    }
}
