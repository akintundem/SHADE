package eventplanner.features.attendee.controller;

import eventplanner.features.attendee.dto.request.BulkAttendeeCreateRequest;
import eventplanner.features.attendee.dto.request.ListAttendeesRequest;
import eventplanner.features.attendee.dto.response.AttendeeResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;
import eventplanner.features.attendee.service.AttendeeInviteService;
import eventplanner.features.attendee.service.AttendeeService;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.UUID;

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
			List<AttendeeResponse> responses = attendeeService.toResponseList(saved);
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
			
			AttendeeResponse response = attendeeService.toResponse(attendee);
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
			Page<AttendeeResponse> responsePage = attendees.map(attendeeService::toResponse);
			return ResponseEntity.ok(responsePage);
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	// ==================== Update Invite RSVP Status ====================

	@PostMapping("/invites")
	@Operation(summary = "Update attendee invite RSVP status", 
		description = "Update attendee invite RSVP status. Can update by inviteId or token (query parameters). Status can be any valid AttendeeInviteStatus (ACCEPTED, DECLINED, REVOKED, EXPIRED). Works for both user-linked attendees and email-only guests.")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE)
	public ResponseEntity<AttendeeResponse> updateInviteStatus(
			@RequestParam(required = false) UUID inviteId,
			@RequestParam(required = false) String token,
			@RequestParam String status,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
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
				AttendeeResponse response = attendeeService.toResponse(attendee);
				return ResponseEntity.ok(response);
			} else {
				// Other statuses - return no content
				return ResponseEntity.noContent().build();
			}
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

}
