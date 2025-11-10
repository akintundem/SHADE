package eventplanner.features.attendee.controller;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.features.attendee.dto.AttendeeCreateRequest;
import eventplanner.features.attendee.dto.request.SendInvitesRequest;
import eventplanner.features.attendee.dto.response.SendInvitesResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.repository.AttendeeRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/attendees")
@Tag(name = "Attendees")
public class AttendeeController {

	private final AttendeeService attendeeService;
	private final AuthorizationService authorizationService;
	private final NotificationService notificationService;
	private final AttendeeRepository attendeeRepository;

	public AttendeeController(
			AttendeeService attendeeService,
			AuthorizationService authorizationService,
			NotificationService notificationService,
			AttendeeRepository attendeeRepository) {
		this.attendeeService = attendeeService;
		this.authorizationService = authorizationService;
		this.notificationService = notificationService;
		this.attendeeRepository = attendeeRepository;
	}

    @PostMapping
    @Operation(summary = "Add attendees")
    @RequiresPermission(RbacPermissions.ATTENDEE_CREATE)
    public ResponseEntity<List<Attendee>> add(
            @Valid @RequestBody List<AttendeeCreateRequest> attendees,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Validate input list
        if (attendees == null || attendees.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Validate list size to prevent abuse
        if (attendees.size() > 100) {
            return ResponseEntity.badRequest().build();
        }
        
        // Verify user can access all events in the request
        // Note: RBAC annotation can't validate a list of events, so we validate each one
        for (AttendeeCreateRequest req : attendees) {
            if (req.getEventId() != null) {
                if (!authorizationService.canAccessEvent(principal, req.getEventId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Access denied to event: " + req.getEventId());
                }
            }
        }
        
        List<Attendee> toSave = attendees.stream().map(req -> {
            Attendee e = new Attendee();
            e.setEventId(req.getEventId());
            e.setName(req.getName());
            e.setEmail(req.getEmail());
			return e;
		}).collect(Collectors.toList());
		return ResponseEntity.ok(attendeeService.addAll(toSave));
    }

	@PostMapping("/invites/send")
	@Operation(summary = "Send invitations")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE, resources = {"event_id=#request.eventId"})
	public ResponseEntity<SendInvitesResponse> sendInvites(
			@Valid @RequestBody SendInvitesRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		
		// Fetch attendees to send invitations to
		List<Attendee> attendees = attendeeRepository.findAllById(request.getAttendeeIds());
		
		// Filter to only attendees for this event
		List<Attendee> eventAttendees = attendees.stream()
			.filter(a -> a.getEventId().equals(request.getEventId()))
			.collect(Collectors.toList());
		
		if (eventAttendees.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
				"No valid attendees found for the specified event");
		}
		
		int queuedCount = 0;
		int failedCount = 0;
		List<UUID> queuedAttendeeIds = new java.util.ArrayList<>();
		List<UUID> failedAttendeeIds = new java.util.ArrayList<>();
		
		// Send invitations via email if requested
		if (Boolean.TRUE.equals(request.getSendEmail())) {
			for (Attendee attendee : eventAttendees) {
				if (attendee.getEmail() == null || attendee.getEmail().trim().isEmpty()) {
					failedCount++;
					failedAttendeeIds.add(attendee.getId());
					continue;
				}
				
				try {
					// Prepare email template variables
					Map<String, Object> templateVariables = new HashMap<>();
					templateVariables.put("attendeeName", attendee.getName() != null ? attendee.getName() : "Guest");
					templateVariables.put("eventId", request.getEventId().toString());
					if (request.getCustomMessage() != null && !request.getCustomMessage().trim().isEmpty()) {
						templateVariables.put("customMessage", request.getCustomMessage());
					}
					
					// Send email invitation
					NotificationRequest notificationRequest = NotificationRequest.builder()
						.type(CommunicationType.EMAIL)
						.to(attendee.getEmail())
						.subject("You're Invited to an Event")
						.templateId("event-invitation") // Template must be created in Resend dashboard
						.templateVariables(templateVariables)
						.eventId(request.getEventId())
						.build();
					
					var notificationResponse = notificationService.send(notificationRequest);
					
					if (notificationResponse.isSuccess()) {
						queuedCount++;
						queuedAttendeeIds.add(attendee.getId());
					} else {
						failedCount++;
						failedAttendeeIds.add(attendee.getId());
					}
				} catch (Exception e) {
					failedCount++;
					failedAttendeeIds.add(attendee.getId());
				}
			}
		}
		
		// TODO: Implement push notification sending if request.getSendPush() is true
		
		SendInvitesResponse response = new SendInvitesResponse();
		response.setStatus(queuedCount > 0 ? "completed" : "failed");
		response.setQueuedCount(queuedCount);
		response.setFailedCount(failedCount);
		response.setQueuedAttendeeIds(queuedAttendeeIds);
		response.setMessage(String.format("Invitations processed: %d queued, %d failed", 
			queuedCount, failedCount));
		return ResponseEntity.ok(response);
	}

	@GetMapping("/event/{eventId}")
	@Operation(summary = "List attendees by event")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#eventId"})
	public ResponseEntity<List<Attendee>> listByEvent(@PathVariable String eventId) {
		try {
			UUID uuid = UUID.fromString(eventId);
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
			Attendee attendee = attendeeRepository.findById(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
					"Attendee not found: " + attendeeId));
			
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
			Attendee attendee = attendeeRepository.findById(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
					"Attendee not found: " + attendeeId));
			
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
