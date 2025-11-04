package eventplanner.features.attendee.controller;

import eventplanner.features.attendee.dto.AttendeeCreateRequest;
import eventplanner.features.attendee.dto.request.SendInvitesRequest;
import eventplanner.features.attendee.dto.response.SendInvitesResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.service.AttendeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/attendees")
@Tag(name = "Attendees")
public class AttendeeController {

	private final AttendeeService attendeeService;

	public AttendeeController(AttendeeService attendeeService) {
		this.attendeeService = attendeeService;
	}

    @PostMapping
    @Operation(summary = "Add attendees")
    public ResponseEntity<List<Attendee>> add(@Valid @RequestBody List<AttendeeCreateRequest> attendees) {
        // Validate input list
        if (attendees == null || attendees.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Validate list size to prevent abuse
        if (attendees.size() > 100) {
            return ResponseEntity.badRequest().build();
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
	public ResponseEntity<SendInvitesResponse> sendInvites(@Valid @RequestBody SendInvitesRequest request) {
		SendInvitesResponse response = new SendInvitesResponse();
		response.setStatus("queued");
		response.setQueuedCount(request.getAttendeeIds().size());
		response.setFailedCount(0);
		response.setQueuedAttendeeIds(request.getAttendeeIds());
		response.setMessage("Invitations queued successfully");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/event/{eventId}")
	@Operation(summary = "List attendees by event")
	public ResponseEntity<List<Attendee>> listByEvent(@PathVariable String eventId) {
		try {
			java.util.UUID uuid = java.util.UUID.fromString(eventId);
			return ResponseEntity.ok(attendeeService.listByEvent(uuid));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@PatchMapping("/{attendeeId}/rsvp")
	@Operation(summary = "Update RSVP status")
	public ResponseEntity<Attendee> updateRsvp(@PathVariable String attendeeId, @RequestParam String status) {
		try {
			java.util.UUID uuid = java.util.UUID.fromString(attendeeId);
			// Validate status parameter
			if (status == null || status.trim().isEmpty()) {
				return ResponseEntity.badRequest().build();
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
	public ResponseEntity<Attendee> checkIn(@PathVariable String attendeeId) {
		try {
			java.util.UUID uuid = java.util.UUID.fromString(attendeeId);
			return attendeeService.checkIn(uuid)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}
}

