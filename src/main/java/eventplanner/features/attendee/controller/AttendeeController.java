package eventplanner.features.attendee.controller;

import eventplanner.features.attendee.dto.BulkAttendeeCreateRequest;
import eventplanner.features.attendee.dto.request.SendInvitesRequest;
import eventplanner.features.attendee.dto.response.SendInvitesResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.attendee.service.AttendeeInvitationService;
import eventplanner.features.attendee.service.AttendeeService;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/attendees")
@Tag(name = "Attendees")
public class AttendeeController {

	private final AttendeeService attendeeService;
	private final AuthorizationService authorizationService;
	private final AttendeeInvitationService attendeeInvitationService;
	private final AttendeeRepository attendeeRepository;

	public AttendeeController(
			AttendeeService attendeeService,
			AuthorizationService authorizationService,
			AttendeeInvitationService attendeeInvitationService,
			AttendeeRepository attendeeRepository) {
		this.attendeeService = attendeeService;
		this.authorizationService = authorizationService;
		this.attendeeInvitationService = attendeeInvitationService;
		this.attendeeRepository = attendeeRepository;
	}

    @PostMapping
    @Operation(summary = "Add attendees", description = "Bulk add attendees to a single event. More efficient than repeating eventId for each attendee.")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE, resources = {"event_id=#request.eventId"})
    public ResponseEntity<List<Attendee>> add(
            @Valid @RequestBody BulkAttendeeCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Validate request
        if (request == null || request.getEventId() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        // Validate attendees list
        if (request.getAttendees() == null || request.getAttendees().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Verify user can access the event
        // RBAC annotation checks permission, but we also validate event access explicitly
        if (!authorizationService.canAccessEvent(principal, request.getEventId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "Access denied to event: " + request.getEventId());
        }
        
        // Map attendee info to Attendee entities
        List<Attendee> toSave = request.getAttendees().stream().map(attendeeInfo -> {
            Attendee attendee = new Attendee();
            attendee.setEventId(request.getEventId());
            attendee.setName(attendeeInfo.getName());
            attendee.setEmail(attendeeInfo.getEmail());
            attendee.setPhone(attendeeInfo.getPhone());
            return attendee;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(attendeeService.addAll(toSave));
    }

	@PostMapping("/invites/send")
	@Operation(summary = "Send invitations", description = "Queues invitations to be sent asynchronously. Returns immediately with a queued status. Invitations are processed in the background in batches to handle large attendee lists efficiently.")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE, resources = {"event_id=#request.eventId"})
	public ResponseEntity<SendInvitesResponse> sendInvites(
			@Valid @RequestBody SendInvitesRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		
		// Verify user can access the event
		if (!authorizationService.canAccessEvent(principal, request.getEventId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
				"Access denied to event: " + request.getEventId());
		}
		
		// Validate request
		if (request.getAttendeeIds() == null || request.getAttendeeIds().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
				"At least one attendee ID is required");
		}
		
		// Validate list size to prevent abuse
		if (request.getAttendeeIds().size() > 1000) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
				"Cannot send invitations to more than 1000 attendees at once");
		}
		
		// Queue invitations for async processing
		boolean sendEmail = Boolean.TRUE.equals(request.getSendEmail());
		boolean sendPush = Boolean.TRUE.equals(request.getSendPush());
		
		if (sendEmail || sendPush) {
			// Start async processing - returns immediately
			attendeeInvitationService.sendInvitationsAsync(
					request.getEventId(),
					request.getAttendeeIds(),
					request.getCustomMessage(),
					sendEmail,
					sendPush
			);
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
				"At least one notification method (email or push) must be enabled");
		}
		
		// Return immediately with queued status
		SendInvitesResponse response = new SendInvitesResponse();
		response.setStatus("queued");
		response.setQueuedCount(0); // Will be updated when processing completes
		response.setFailedCount(0);
		response.setQueuedAttendeeIds(new ArrayList<>());
		response.setFailedAttendeeIds(new ArrayList<>());
		response.setMessage(String.format("Invitations queued for processing: %d attendees", 
			request.getAttendeeIds().size()));
		return ResponseEntity.accepted().body(response);
	}

	@GetMapping("/event/{eventId}")
	@Operation(summary = "List attendees by event")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#eventId"})
	public ResponseEntity<List<Attendee>> listByEvent(
			@PathVariable String eventId,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			UUID uuid = UUID.fromString(eventId);
			// Explicitly verify event access in addition to RBAC
			// RBAC checks permission, but we also validate event ownership/membership
			if (!authorizationService.canAccessEvent(principal, uuid)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + eventId);
			}
			return ResponseEntity.ok(attendeeService.listByEvent(uuid));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@PatchMapping("/{attendeeId}/rsvp")
	@Operation(summary = "Update RSVP status")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_UPDATE, resources = {"attendance_id=#attendeeId"})
	public ResponseEntity<Attendee> updateRsvp(
			@PathVariable String attendeeId, 
			@RequestParam String status,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			UUID uuid = UUID.fromString(attendeeId);
			
			// Validate status parameter
			if (status == null || status.trim().isEmpty()) {
				return ResponseEntity.badRequest().build();
			}
			
			// Verify attendee exists and user can access the event
			// RBAC checks permission but we need to validate event access via attendee's eventId
			// This prevents unauthorized access by guessing UUIDs
			Attendee attendee = attendeeRepository.findById(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
					"Attendee not found: " + attendeeId));
			
			// Validate event ownership/membership - ensures user has proper access to the event
			if (attendee.getEventId() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
					"Attendee is not associated with an event");
			}
			
			if (!authorizationService.canAccessEvent(principal, attendee.getEventId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + attendee.getEventId());
			}
			
			return attendeeService.updateRsvp(uuid, status)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@PostMapping("/{attendeeId}/check-in")
	@Operation(summary = "Check-in attendee")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CHECKIN, resources = {"attendance_id=#attendeeId"})
	public ResponseEntity<Attendee> checkIn(
			@PathVariable String attendeeId,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			UUID uuid = UUID.fromString(attendeeId);
			
			// Verify attendee exists and user can access the event
			// RBAC checks permission but we need to validate event access via attendee's eventId
			// This prevents unauthorized access by guessing UUIDs
			Attendee attendee = attendeeRepository.findById(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
					"Attendee not found: " + attendeeId));
			
			// Validate event ownership/membership - ensures user has proper access to the event
			if (attendee.getEventId() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
					"Attendee is not associated with an event");
			}
			
			if (!authorizationService.canAccessEvent(principal, attendee.getEventId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + attendee.getEventId());
			}
			
			return attendeeService.checkIn(uuid)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}
}
