package eventplanner.features.attendee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.features.attendee.dto.BulkAttendeeCreateRequest;
import eventplanner.features.attendee.dto.request.SendInvitesRequest;
import eventplanner.features.attendee.dto.request.UpdateAttendeeRequest;
import eventplanner.features.attendee.dto.response.AttendeeResponse;
import eventplanner.features.attendee.dto.response.SendInvitesResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.entity.AttendeeStatus;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.attendee.service.AttendeeAuditService;
import eventplanner.features.attendee.service.AttendeeIdempotencyService;
import eventplanner.features.attendee.service.AttendeeInvitationService;
import eventplanner.features.attendee.service.AttendeeService;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for attendee management with full CRUD operations
 * Returns DTOs instead of JPA entities to avoid leaking internal fields
 * Supports pagination, filtering, sorting, and idempotency
 */
@RestController
@RequestMapping("/api/v1/attendees")
@Tag(name = "Attendees")
@RequiredArgsConstructor
@Slf4j
public class AttendeeController {

	private final AttendeeService attendeeService;
	private final AuthorizationService authorizationService;
	private final AttendeeInvitationService attendeeInvitationService;
	private final AttendeeRepository attendeeRepository;
	private final AttendeeAuditService attendeeAuditService;
	private final AttendeeIdempotencyService idempotencyService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	// ==================== Individual Attendee CRUD Operations ====================

	@GetMapping("/{id}")
	@Operation(summary = "Get attendee by ID", description = "Retrieve a single attendee by their ID")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"attendance_id=#id"})
	public ResponseEntity<AttendeeResponse> getAttendee(
			@PathVariable String id,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			UUID attendeeId = UUID.fromString(id);
			
			Optional<Attendee> attendeeOpt = attendeeService.getById(attendeeId);
			if (attendeeOpt.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found: " + id);
			}
			
			Attendee attendee = attendeeOpt.get();
			
			// Verify user can access the event this attendee belongs to
			if (!authorizationService.canAccessEvent(principal, attendee.getEventId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + attendee.getEventId());
			}
			
			AttendeeResponse response = attendeeService.toResponse(attendee);
			return ResponseEntity.ok(response);
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendee ID format");
		}
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update attendee", description = "Update attendee information")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_UPDATE, resources = {"attendance_id=#id"})
	public ResponseEntity<AttendeeResponse> updateAttendee(
			@PathVariable String id,
			@Valid @RequestBody UpdateAttendeeRequest request,
			@AuthenticationPrincipal UserPrincipal principal,
			HttpServletRequest httpRequest) {
		try {
			UUID attendeeId = UUID.fromString(id);
			
			// Get existing attendee
			Optional<Attendee> existingOpt = attendeeService.getById(attendeeId);
			if (existingOpt.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found: " + id);
			}
			
			Attendee existing = existingOpt.get();
			
			// Verify user can access the event
			if (!authorizationService.canAccessEvent(principal, existing.getEventId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + existing.getEventId());
			}
			
			// Map request to entity
			Attendee updates = new Attendee();
			updates.setName(request.getName());
			updates.setEmail(request.getEmail());
			updates.setPhone(request.getPhone());
			updates.setRsvpStatus(request.getRsvpStatus());
			updates.setEmailConsent(request.getEmailConsent());
			updates.setSmsConsent(request.getSmsConsent());
			updates.setDataProcessingConsent(request.getDataProcessingConsent());
			
			// Store old value for audit
			String oldValue = String.format("name=%s,email=%s,phone=%s,status=%s", 
				existing.getName(), existing.getEmail(), existing.getPhone(), existing.getRsvpStatus());
			
			// Update
			Optional<Attendee> updated = attendeeService.update(attendeeId, updates);
			if (updated.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found: " + id);
			}
			
			Attendee result = updated.get();
			
			// Store new value for audit
			String newValue = String.format("name=%s,email=%s,phone=%s,status=%s", 
				result.getName(), result.getEmail(), result.getPhone(), result.getRsvpStatus());
			
			// Audit log
			attendeeAuditService.logAttendeeUpdated(
				attendeeId, 
				existing.getEventId(), 
				principal.getUser().getId(), 
				principal.getUser().getEmail(),
				getClientIp(httpRequest),
				httpRequest.getHeader("User-Agent"),
				oldValue,
				newValue,
				null
			);
			
			AttendeeResponse response = attendeeService.toResponse(result);
			return ResponseEntity.ok(response);
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete attendee", description = "Remove an attendee from an event")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_DELETE, resources = {"attendance_id=#id"})
	public ResponseEntity<Void> deleteAttendee(
			@PathVariable String id,
			@AuthenticationPrincipal UserPrincipal principal,
			HttpServletRequest httpRequest) {
		try {
			UUID attendeeId = UUID.fromString(id);
			
			// Get attendee first to verify event access and for audit
			Optional<Attendee> attendeeOpt = attendeeService.getById(attendeeId);
			if (attendeeOpt.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found: " + id);
			}
			
			Attendee attendee = attendeeOpt.get();
			
			// Verify user can access the event
			if (!authorizationService.canAccessEvent(principal, attendee.getEventId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + attendee.getEventId());
			}
			
			// Delete
			boolean deleted = attendeeService.delete(attendeeId);
			if (!deleted) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found: " + id);
			}
			
			// Audit log
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("attendeeName", attendee.getName());
			metadata.put("attendeeEmail", attendee.getEmail());
			
			attendeeAuditService.logAttendeeDeleted(
				attendeeId, 
				attendee.getEventId(), 
				principal.getUser().getId(), 
				principal.getUser().getEmail(),
				getClientIp(httpRequest),
				httpRequest.getHeader("User-Agent"),
				metadata
			);
			
			return ResponseEntity.noContent().build();
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendee ID format");
		}
	}

	// ==================== Bulk Operations ====================

	@PostMapping
	@Operation(summary = "Add attendees", description = "Bulk add attendees to a single event. Returns DTOs instead of entities.")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE, resources = {"event_id=#request.eventId"})
	public ResponseEntity<List<AttendeeResponse>> add(
			@Valid @RequestBody BulkAttendeeCreateRequest request,
			@AuthenticationPrincipal UserPrincipal principal,
			HttpServletRequest httpRequest) {
		// Validate request
		if (request == null || request.getEventId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request");
		}
		
		// Validate attendees list
		if (request.getAttendees() == null || request.getAttendees().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendees list cannot be empty");
		}
		
		// Verify user can access the event
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
		
		List<Attendee> saved = attendeeService.addAll(toSave);
		
		// Audit log
		attendeeAuditService.logBulkImport(
			request.getEventId(),
			principal.getUser().getId(),
			principal.getUser().getEmail(),
			getClientIp(httpRequest),
			httpRequest.getHeader("User-Agent"),
			saved.size(),
			null
		);
		
		// Convert to DTOs
		List<AttendeeResponse> responses = attendeeService.toResponseList(saved);
		return ResponseEntity.ok(responses);
	}

	// ==================== List & Filter Operations ====================

	@GetMapping("/event/{eventId}")
	@Operation(summary = "List attendees by event with pagination and filtering", 
		description = "Retrieve attendees for an event with support for pagination, status filters, search, and sorting")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#eventId"})
	public ResponseEntity<Page<AttendeeResponse>> listByEvent(
			@PathVariable String eventId,
			@RequestParam(required = false) @Parameter(description = "Filter by RSVP status (comma-separated): PENDING,CONFIRMED,DECLINED,TENTATIVE,NO_SHOW") 
				String status,
			@RequestParam(required = false) @Parameter(description = "Filter by check-in status: true (checked in) or false (not checked in)") 
				Boolean checkedIn,
			@RequestParam(required = false) @Parameter(description = "Search by name or email") 
				String search,
			@RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-indexed)") 
				int page,
			@RequestParam(defaultValue = "20") @Parameter(description = "Page size") 
				int size,
			@RequestParam(defaultValue = "name") @Parameter(description = "Sort field: name, email, rsvpStatus, checkedInAt, createdAt") 
				String sortBy,
			@RequestParam(defaultValue = "ASC") @Parameter(description = "Sort direction: ASC or DESC") 
				String sortDirection,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			UUID uuid = UUID.fromString(eventId);
			
			// Verify event access
			if (!authorizationService.canAccessEvent(principal, uuid)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + eventId);
			}
			
			// Validate pagination parameters
			if (page < 0) page = 0;
			if (size < 1) size = 20;
			if (size > 100) size = 100; // Max page size
			
			// Create sort
			Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
			Sort sort = Sort.by(direction, sortBy);
			Pageable pageable = PageRequest.of(page, size, sort);
			
			Page<Attendee> attendees;
			
			// Apply search if provided
			if (StringUtils.hasText(search)) {
				attendees = attendeeService.searchAttendees(uuid, search.trim(), pageable);
			}
			// Apply check-in filter if provided
			else if (checkedIn != null) {
				if (checkedIn) {
					attendees = attendeeService.listCheckedIn(uuid, pageable);
				} else {
					attendees = attendeeRepository.findNotCheckedInByEventId(uuid, pageable);
				}
			}
			// Apply status filter if provided
			else if (StringUtils.hasText(status)) {
				List<AttendeeStatus> statuses = parseStatuses(status);
				if (statuses.isEmpty()) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status values");
				}
				attendees = attendeeService.listByEventAndStatuses(uuid, statuses, pageable);
			}
			// No filters - return all
			else {
				attendees = attendeeService.listByEventPaginated(uuid, pageable);
			}
			
			// Convert to DTOs
			Page<AttendeeResponse> responses = attendeeService.toResponsePage(attendees);
			return ResponseEntity.ok(responses);
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event ID or parameters: " + e.getMessage());
		}
	}

	// ==================== RSVP & Check-in Operations ====================

	@PatchMapping("/events/{eventId}/attendees/{attendeeId}/rsvp")
	@Operation(summary = "Update RSVP status", description = "Update attendee RSVP status using validated enum")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_UPDATE, resources = {"attendance_id=#attendeeId", "event_id=#eventId"})
	public ResponseEntity<AttendeeResponse> updateRsvp(
			@PathVariable String eventId,
			@PathVariable String attendeeId, 
			@RequestParam @Parameter(description = "RSVP status: PENDING, CONFIRMED, DECLINED, TENTATIVE, NO_SHOW") String status,
			@AuthenticationPrincipal UserPrincipal principal,
			HttpServletRequest httpRequest) {
		try {
			UUID attendeeUuid = UUID.fromString(attendeeId);
			UUID eventUuid = UUID.fromString(eventId);
			
			// Validate and parse status
			if (!AttendeeStatus.isValid(status)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
					"Invalid status. Valid values are: PENDING, CONFIRMED, DECLINED, TENTATIVE, NO_SHOW");
			}
			AttendeeStatus attendeeStatus = AttendeeStatus.fromString(status);
			
			// Verify attendee exists and belongs to the specified event
			Attendee attendee = attendeeRepository.findById(attendeeUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
					"Attendee not found: " + attendeeId));
			
			// Verify attendee belongs to the specified event
			if (!eventUuid.equals(attendee.getEventId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
					"Attendee does not belong to the specified event");
			}
			
			AttendeeStatus oldStatus = attendee.getRsvpStatus();
			
			Optional<Attendee> updated = attendeeService.updateRsvp(attendeeUuid, attendeeStatus);
			if (updated.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found: " + attendeeId);
			}
			
			// Audit log
			attendeeAuditService.logRsvpUpdated(
				attendeeUuid,
				eventUuid,
				principal.getUser().getId(),
				principal.getUser().getEmail(),
				getClientIp(httpRequest),
				httpRequest.getHeader("User-Agent"),
				oldStatus != null ? oldStatus.name() : null,
				attendeeStatus.name(),
				null
			);
			
			AttendeeResponse response = attendeeService.toResponse(updated.get());
			return ResponseEntity.ok(response);
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	@PostMapping("/events/{eventId}/attendees/{attendeeId}/check-in")
	@Operation(summary = "Check-in attendee", description = "Check-in attendee with idempotency support")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_CHECKIN, resources = {"attendance_id=#attendeeId", "event_id=#eventId"})
	public ResponseEntity<AttendeeResponse> checkIn(
			@PathVariable String eventId,
			@PathVariable String attendeeId,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@RequestHeader(value = "X-Device-ID", required = false) String deviceId,
			@AuthenticationPrincipal UserPrincipal principal,
			HttpServletRequest httpRequest) {
		try {
			UUID attendeeUuid = UUID.fromString(attendeeId);
			UUID eventUuid = UUID.fromString(eventId);
			
			// Check idempotency - return cached result if available
			if (StringUtils.hasText(idempotencyKey)) {
				Optional<String> cachedResult = idempotencyService.getProcessedResult(idempotencyKey);
				if (cachedResult.isPresent()) {
					AttendeeResponse cached = objectMapper.readValue(cachedResult.get(), AttendeeResponse.class);
					return ResponseEntity.ok()
						.header("X-Idempotency-Replay", "true")
						.body(cached);
				}
				
				// Mark as processing
				if (!idempotencyService.markAsProcessing(idempotencyKey)) {
					throw new ResponseStatusException(HttpStatus.CONFLICT, 
						"Check-in already in progress. Please wait.");
				}
			}
			
			try {
				// Verify attendee exists and belongs to the specified event
				Attendee attendee = attendeeRepository.findById(attendeeUuid)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
						"Attendee not found: " + attendeeId));
				
				// Verify attendee belongs to the specified event
				if (!eventUuid.equals(attendee.getEventId())) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
						"Attendee does not belong to the specified event");
				}
				
				Optional<Attendee> checkedIn = attendeeService.checkIn(attendeeUuid);
				if (checkedIn.isEmpty()) {
					throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found: " + attendeeId);
				}
				
				Attendee result = checkedIn.get();
				
				// Audit log with idempotency key and device
				Map<String, Object> metadata = new HashMap<>();
				metadata.put("attendeeName", result.getName());
				metadata.put("attendeeEmail", result.getEmail());
				
				attendeeAuditService.logCheckIn(
					attendeeUuid,
					eventUuid,
					principal.getUser().getId(),
					principal.getUser().getEmail(),
					getClientIp(httpRequest),
					httpRequest.getHeader("User-Agent"),
					deviceId,
					idempotencyKey,
					metadata
				);
				
				AttendeeResponse response = attendeeService.toResponse(result);
				
				// Store result for idempotency
				if (StringUtils.hasText(idempotencyKey)) {
					try {
						String resultJson = objectMapper.writeValueAsString(response);
						idempotencyService.storeResult(idempotencyKey, resultJson);
					} catch (Exception e) {
						log.error("Failed to store idempotency result: {}", e.getMessage());
					} finally {
						idempotencyService.releaseProcessingLock(idempotencyKey);
					}
				}
				
				return ResponseEntity.ok(response);
				
			} catch (Exception e) {
				// Release lock on error
				if (StringUtils.hasText(idempotencyKey)) {
					idempotencyService.releaseProcessingLock(idempotencyKey);
				}
				throw e;
			}
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			log.error("Error during check-in: {}", e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during check-in");
		}
	}

	// ==================== Invitation Operations ====================

	@PostMapping("/invites/send")
	@Operation(summary = "Send invitations with idempotency", 
		description = "Queues invitations to be sent asynchronously. Returns immediately with a queued status. Supports idempotency to prevent duplicate sends.")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE, resources = {"event_id=#request.eventId"})
	public ResponseEntity<SendInvitesResponse> sendInvites(
			@Valid @RequestBody SendInvitesRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@RequestHeader(value = "X-Device-ID", required = false) String deviceId,
			@AuthenticationPrincipal UserPrincipal principal,
			HttpServletRequest httpRequest) {
		
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
		
		// Check idempotency - return cached result if available
		if (StringUtils.hasText(idempotencyKey)) {
			Optional<String> cachedResult = idempotencyService.getProcessedResult(idempotencyKey);
			if (cachedResult.isPresent()) {
				try {
					SendInvitesResponse cached = objectMapper.readValue(cachedResult.get(), SendInvitesResponse.class);
					return ResponseEntity.accepted()
						.header("X-Idempotency-Replay", "true")
						.body(cached);
				} catch (Exception e) {
					log.error("Failed to deserialize cached result: {}", e.getMessage());
				}
			}
			
			// Mark as processing
			if (!idempotencyService.markAsProcessing(idempotencyKey)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, 
					"Invitation send already in progress. Please wait.");
			}
		}
		
		try {
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
			
			// Audit log
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("attendeeCount", request.getAttendeeIds().size());
			metadata.put("sendEmail", sendEmail);
			metadata.put("sendPush", sendPush);
			
			attendeeAuditService.logInvitationQueued(
				request.getEventId(),
				principal.getUser().getId(),
				principal.getUser().getEmail(),
				getClientIp(httpRequest),
				httpRequest.getHeader("User-Agent"),
				idempotencyKey,
				metadata
			);
			
			// Return immediately with queued status
			SendInvitesResponse response = new SendInvitesResponse();
			response.setStatus("queued");
			response.setQueuedCount(0); // Will be updated when processing completes
			response.setFailedCount(0);
			response.setQueuedAttendeeIds(new ArrayList<>());
			response.setFailedAttendeeIds(new ArrayList<>());
			response.setMessage(String.format("Invitations queued for processing: %d attendees", 
				request.getAttendeeIds().size()));
			
			// Store result for idempotency
			if (StringUtils.hasText(idempotencyKey)) {
				try {
					String resultJson = objectMapper.writeValueAsString(response);
					idempotencyService.storeResult(idempotencyKey, resultJson);
				} catch (Exception e) {
					log.error("Failed to store idempotency result: {}", e.getMessage());
				} finally {
					idempotencyService.releaseProcessingLock(idempotencyKey);
				}
			}
			
			return ResponseEntity.accepted().body(response);
			
		} catch (Exception e) {
			// Release lock on error
			if (StringUtils.hasText(idempotencyKey)) {
				idempotencyService.releaseProcessingLock(idempotencyKey);
			}
			throw e;
		}
	}

	// ==================== Helper Methods ====================

	/**
	 * Extract client IP address from request
	 */
	private String getClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(forwarded)) {
			return forwarded.split(",")[0].trim();
		}
		String realIp = request.getHeader("X-Real-IP");
		if (StringUtils.hasText(realIp)) {
			return realIp.trim();
		}
		return request.getRemoteAddr();
	}

	/**
	 * Parse comma-separated status values
	 */
	private List<AttendeeStatus> parseStatuses(String statusParam) {
		if (!StringUtils.hasText(statusParam)) {
			return Collections.emptyList();
		}
		
		List<AttendeeStatus> statuses = new ArrayList<>();
		String[] parts = statusParam.toUpperCase().split(",");
		
		for (String part : parts) {
			String trimmed = part.trim();
			if (AttendeeStatus.isValid(trimmed)) {
				statuses.add(AttendeeStatus.fromString(trimmed));
			} else {
				log.warn("Invalid status value: {}", trimmed);
			}
		}
		
		return statuses;
	}
}
