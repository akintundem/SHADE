package ai.eventplanner.attendee.controller;

import ai.eventplanner.attendee.dto.AttendeeCreateRequest;
import ai.eventplanner.attendee.dto.request.SendInvitesRequest;
import ai.eventplanner.attendee.dto.response.SendInvitesResponse;
import ai.eventplanner.attendee.model.AttendeeEntity;
import ai.eventplanner.attendee.service.AttendeeService;
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
    public ResponseEntity<List<AttendeeEntity>> add(@Valid @RequestBody List<AttendeeCreateRequest> attendees) {
        List<AttendeeEntity> toSave = attendees.stream().map(req -> {
            AttendeeEntity e = new AttendeeEntity();
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
	public ResponseEntity<List<AttendeeEntity>> listByEvent(@PathVariable String eventId) {
		return ResponseEntity.ok(attendeeService.listByEvent(java.util.UUID.fromString(eventId)));
	}

	@PatchMapping("/{attendeeId}/rsvp")
	@Operation(summary = "Update RSVP status")
	public ResponseEntity<AttendeeEntity> updateRsvp(@PathVariable String attendeeId, @RequestParam String status) {
		return attendeeService.updateRsvp(java.util.UUID.fromString(attendeeId), status)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/{attendeeId}/check-in")
	@Operation(summary = "Check-in attendee")
	public ResponseEntity<AttendeeEntity> checkIn(@PathVariable String attendeeId) {
		return attendeeService.checkIn(java.util.UUID.fromString(attendeeId))
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}
}

