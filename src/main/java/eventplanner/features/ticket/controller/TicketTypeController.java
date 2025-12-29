package eventplanner.features.ticket.controller;

import eventplanner.features.ticket.dto.request.CreateTicketTypeRequest;
import eventplanner.features.ticket.dto.request.UpdateTicketTypeRequest;
import eventplanner.features.ticket.dto.response.TicketTypeResponse;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.enums.TicketTypeCategory;
import eventplanner.features.ticket.service.TicketTypeService;
import eventplanner.security.authorization.rbac.RbacPermissions;
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
        try {
            // Verify user can access the event
            if (!authorizationService.canAccessEvent(principal, eventId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Access denied to event: " + eventId);
            }

            // Ensure eventId in request matches path variable
            TicketType newTicketType = ticketTypeService.createTicketType(eventId, request, principal);
            return new ResponseEntity<>(TicketTypeResponse.from(newTicketType), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
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
        try {
            // Verify user can access the event
            if (!authorizationService.canAccessEvent(principal, eventId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Access denied to event: " + eventId);
            }

            List<TicketTypeResponse> ticketTypes = ticketTypeService.getTicketTypes(eventId, id, category, activeOnly, name);
            return ResponseEntity.ok(ticketTypes);
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
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
        try {
            // Verify user can access the event
            if (!authorizationService.canAccessEvent(principal, eventId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Access denied to event: " + eventId);
            }

            TicketType updatedTicketType = ticketTypeService.updateTicketType(ticketTypeId, eventId, request, ifMatch, principal);
            return ResponseEntity.ok()
                .eTag(String.valueOf(updatedTicketType.getVersion())) // Return ETag with current version
                .body(TicketTypeResponse.from(updatedTicketType));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Ticket type has been modified by another user. Please refresh and try again.", e);
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
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
        @AuthenticationPrincipal UserPrincipal principal) {
        try {
            // Verify user can access the event
            if (!authorizationService.canAccessEvent(principal, eventId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Access denied to event: " + eventId);
            }

            ticketTypeService.deleteTicketType(ticketTypeId, eventId, principal);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
}
