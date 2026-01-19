package eventplanner.features.attendee.controller;

import eventplanner.features.attendee.dto.request.BulkAttendeeCreateRequest;
import eventplanner.features.attendee.dto.request.ListAttendeesRequest;
import eventplanner.features.attendee.dto.response.AttendeeResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;
import eventplanner.features.attendee.service.AttendeeInviteService;
import eventplanner.features.attendee.service.AttendeeService;
import eventplanner.features.event.dto.request.EventListRequest;
import eventplanner.features.event.dto.response.EventResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.service.EventService;
import eventplanner.features.ticket.dto.response.TicketResponse;
import eventplanner.features.ticket.enums.TicketStatus;
import eventplanner.features.ticket.service.TicketService;
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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for attendee management operations
 */
@RestController
@RequestMapping("/api/v1/attendees")
@Tag(name = "Attendees")
@RequiredArgsConstructor
public class AttendeeController {

	private final AttendeeService attendeeService;
	private final AttendeeInviteService inviteService;
	private final AuthorizationService authorizationService;
	private final TicketService ticketService;
	private final EventService eventService;

	// ==================== Individual Attendee CRUD Operations ====================

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete attendee", description = "Remove an attendee from an event. Works for both user-linked attendees (added by userId) and email-only guests (added by email).")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_DELETE, resources = {"attendance_id=#id"})
	public ResponseEntity<Void> deleteAttendee(
			@PathVariable String id,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			UUID attendeeId = UUID.fromString(id);
			
			// Get attendee and verify access
			Attendee attendee = attendeeService.getAttendeeById(attendeeId);
			if (attendee.getEvent() == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found for attendee");
			}
			UUID eventId = attendee.getEvent().getId();
			if (!authorizationService.canAccessEvent(principal, eventId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + eventId);
			}
			
			// Delete
			boolean deleted = attendeeService.delete(attendeeId);
			if (!deleted) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found: " + id);
			}
			
			return ResponseEntity.noContent().build();
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	// ==================== Add Attendees (Single or Bulk) ====================

	@PostMapping
	@Operation(summary = "Add attendees", 
		description = "Add one or more attendees to an event. Supports adding by userId (from directory) or email. Works for both single and multiple attendees. Optional notification preferences allow event owner to send email, SMS, or push notifications to newly added attendees.")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE, resources = {"event_id=#request.eventId"})
	public ResponseEntity<List<AttendeeResponse>> add(
			@Valid @RequestBody BulkAttendeeCreateRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			// Verify user can access the event
			if (!authorizationService.canAccessEvent(principal, request.getEventId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + request.getEventId());
			}
			
			// Create attendees (service handles validation and user resolution)
			List<Attendee> saved = attendeeService.createFromBulkRequest(request);
			
			// Convert to DTOs
			List<AttendeeResponse> responses = saved.stream()
					.map(AttendeeResponse::from)
					.collect(java.util.stream.Collectors.toList());
			return ResponseEntity.ok(responses);
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	// ==================== Get Single Attendee ====================

	@GetMapping("/{id}")
	@Operation(summary = "Get attendee by ID", description = "Retrieve detailed information about a specific attendee. Returns both user-linked attendees (with userId) and email-only guests (without userId).")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"attendance_id=#id"})
	public ResponseEntity<AttendeeResponse> getAttendee(
			@PathVariable String id,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			UUID attendeeId = UUID.fromString(id);
			Attendee attendee = attendeeService.getAttendeeById(attendeeId);
			
			// Verify user can access the event
			if (attendee.getEvent() == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found for attendee");
			}
			UUID eventId = attendee.getEvent().getId();
			if (!authorizationService.canAccessEvent(principal, eventId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + eventId);
			}
			
			AttendeeResponse response = AttendeeResponse.from(attendee);
			return ResponseEntity.ok(response);
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	// ==================== List/Filter Attendees ====================

	@GetMapping
	@Operation(summary = "List or filter attendees", 
		description = "List and filter attendees for an event with pagination. Returns a combination of both user-linked attendees (added by userId) and email-only guests (added by email). Requires eventId as query parameter. Supports filtering by status, check-in status, search, userId, and email.")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#request.eventId"})
	public ResponseEntity<Page<AttendeeResponse>> listAttendees(
			@Valid @ModelAttribute ListAttendeesRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			if (request.getEventId() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event ID is required");
			}
			
			// Verify event access
			if (!authorizationService.canAccessEvent(principal, request.getEventId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + request.getEventId());
			}
			
			// Filter attendees (service handles all filtering logic)
			Page<Attendee> attendees = attendeeService.filterAttendees(
				request.getEventId(),
				request.getStatus(),
				request.getCheckedIn(),
				request.getSearch(),
				request.getUserId(),
				request.getEmail(),
				request.getPage() != null ? request.getPage() : 0,
				request.getSize() != null ? request.getSize() : 20,
				request.getSortBy() != null ? request.getSortBy() : "name",
				request.getSortDirection() != null ? request.getSortDirection() : "ASC");
			
			// Convert to DTOs with pagination
			Page<AttendeeResponse> responsePage = attendees.map(AttendeeResponse::from);
			return ResponseEntity.ok(responsePage);
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

    // ==================== Update Invite RSVP Status ====================

    @PostMapping("/invites")
    @Operation(summary = "Update attendee invite RSVP status", 
        description = "Update attendee invite RSVP status. Can update by inviteId or token (query parameters). Status can be any valid AttendeeInviteStatus (ACCEPTED, DECLINED, REVOKED, EXPIRED). Works for both user-linked attendees and email-only guests.")
    public ResponseEntity<AttendeeResponse> updateInviteStatus(
            @RequestParam(required = false) UUID inviteId,
            @RequestParam(required = false) String token,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            if (principal == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }

            if (inviteId == null && (token == null || token.trim().isEmpty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Either inviteId or token must be provided");
            }
			
			if (status == null || status.trim().isEmpty()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
					"Status is required");
			}
			
			// Parse status
			AttendeeInviteStatus inviteStatus;
			try {
				inviteStatus = AttendeeInviteStatus.valueOf(status.toUpperCase().trim());
			} catch (IllegalArgumentException e) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
					"Invalid status. Valid values are: ACCEPTED, DECLINED, REVOKED, EXPIRED");
			}
			
			Attendee attendee = inviteService.updateInviteStatus(inviteId, token, inviteStatus, principal);
			
			if (inviteStatus == AttendeeInviteStatus.ACCEPTED) {
				// Return attendee response
				AttendeeResponse response = AttendeeResponse.from(attendee);
				return ResponseEntity.ok(response);
			} else {
				// Other statuses - return no content
				return ResponseEntity.noContent().build();
			}
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	// ==================== Get Tickets for Attendee ====================

	@GetMapping("/{id}/tickets")
	@Operation(summary = "Get tickets for attendee", 
		description = "Get all tickets issued to a specific attendee. Optionally filter by event and status.")
	@RequiresPermission(value = RbacPermissions.TICKET_READ, resources = {"attendance_id=#id"})
	public ResponseEntity<List<TicketResponse>> getTicketsByAttendee(
			@PathVariable String id,
			@RequestParam(required = false) UUID eventId,
			@RequestParam(required = false) TicketStatus status,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			UUID attendeeId = UUID.fromString(id);
			
			// Verify attendee exists and user can access the event
			Attendee attendee = attendeeService.getAttendeeById(attendeeId);
			if (attendee.getEvent() == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found for attendee");
			}
			UUID attendeeEventId = attendee.getEvent().getId();
			if (!authorizationService.canAccessEvent(principal, attendeeEventId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + attendeeEventId);
			}
			
			List<TicketResponse> tickets = ticketService.getTicketsByAttendeeId(attendeeId);
			
			// Filter by eventId if provided
			if (eventId != null) {
				tickets = tickets.stream()
					.filter(t -> eventId.equals(t.getEventId()))
					.collect(Collectors.toList());
			}
			
			// Filter by status if provided
			if (status != null) {
				tickets = tickets.stream()
					.filter(t -> status.equals(t.getStatus()))
					.collect(Collectors.toList());
			}

			return ResponseEntity.ok(tickets);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	// ==================== Get Invited Events ====================

	@GetMapping("/invitations")
	@RequiresPermission(RbacPermissions.MY_EVENTS_SEARCH)
	@Operation(summary = "Get invited events", description = "Get events where the current user has been invited as an attendee.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Invited events retrieved successfully"),
		@ApiResponse(responseCode = "401", description = "Unauthorized")
	})
	public ResponseEntity<Page<EventResponse>> getInvitedEvents(
			@Valid EventListRequest request,
			@AuthenticationPrincipal UserPrincipal user) {
		try {
			if (user == null) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
			}
			EventListRequest req = request != null ? request : new EventListRequest();
			Integer page = req.getPage() != null ? req.getPage() : 0;
			Integer size = req.getSize() != null ? req.getSize() : 20;
			Page<Event> events = attendeeService.getInvitedEvents(user.getId(), page, size);
			Page<EventResponse> responses = events.map(event -> eventService.toResponse(event, user));
			return ResponseEntity.ok(responses);
		} catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
	}

	// ==================== RSVP Operations ====================

	@PostMapping("/events/{id}/rsvp")
	@RequiresPermission(RbacPermissions.PUBLIC_EVENTS_SEARCH)
	@Operation(summary = "RSVP to event", description = "RSVP to an event that requires RSVP. Creates or updates attendee record with CONFIRMED status.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "RSVP successful",
				content = @Content(schema = @Schema(implementation = EventResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request or event does not require RSVP"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "404", description = "Event not found")
	})
	public ResponseEntity<EventResponse> rsvp(
			@Parameter(description = "Event ID") @PathVariable UUID id,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			if (principal == null) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
			}
			
			// Use AttendeeService directly
			attendeeService.rsvpToEvent(id, principal.getId());
			
			// Return updated event
			Optional<Event> eventOpt = eventService.getById(id);
			if (eventOpt.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
			}
			EventResponse response = eventService.toResponse(eventOpt.get(), principal);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
	}

}
