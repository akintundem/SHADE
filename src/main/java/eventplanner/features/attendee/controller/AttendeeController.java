package eventplanner.features.attendee.controller;

import eventplanner.features.attendee.dto.BulkAttendeeCreateRequest;
import eventplanner.features.attendee.dto.request.UpdateAttendeeRequest;
import eventplanner.features.attendee.dto.response.AttendeeResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.common.audit.service.AuditLogService;
import eventplanner.common.domain.enums.ActionType;
import eventplanner.features.attendee.service.AttendeeService;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
	private final AttendeeRepository attendeeRepository;
	private final EventRepository eventRepository;
	private final UserAccountRepository userAccountRepository;
	private final AuditLogService auditLogService;

	// ==================== Individual Attendee CRUD Operations ====================

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
			if (existing.getEvent() == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found for attendee");
			}
			UUID eventId = existing.getEvent().getId();
			if (!authorizationService.canAccessEvent(principal, eventId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					"Access denied to event: " + eventId);
			}
			
			// Map request to entity
			Attendee updates = new Attendee();
			updates.setName(request.getName());
			updates.setEmail(request.getEmail());
			updates.setRsvpStatus(request.getRsvpStatus());
			
			// Store old value for audit
			String oldValue = String.format("name=%s,email=%s,status=%s", 
				existing.getName(), existing.getEmail(), existing.getRsvpStatus());
			
			// Update
			Optional<Attendee> updated = attendeeService.update(attendeeId, updates);
			if (updated.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found: " + id);
			}
			
			Attendee result = updated.get();
			
			// Store new value for audit
			String newValue = String.format("name=%s,email=%s,status=%s", 
				result.getName(), result.getEmail(), result.getRsvpStatus());
			
			// Audit log
			auditLogService.builder()
				.domain("ATTENDEE")
				.entityType("Attendee")
				.entityId(attendeeId)
				.action(ActionType.UPDATE)
				.user(principal.getUser().getId(), null, principal.getUser().getEmail())
				.description("Attendee updated")
				.request(getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null)
				.eventId(eventId)
				.oldValue(oldValue)
				.newValue(newValue)
				.log();
			
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
			
			// Audit log
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("attendeeName", attendee.getName());
			metadata.put("attendeeEmail", attendee.getEmail());
			
			auditLogService.builder()
				.domain("ATTENDEE")
				.entityType("Attendee")
				.entityId(attendeeId)
				.action(ActionType.DELETE)
				.user(principal.getUser().getId(), null, principal.getUser().getEmail())
				.description("Attendee deleted")
				.request(getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null)
				.eventId(eventId)
				.metadata(metadata)
				.log();
			
			return ResponseEntity.noContent().build();
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendee ID format");
		}
	}

	// ==================== Add Attendees (Single or Bulk) ====================

	@PostMapping
	@Operation(summary = "Add attendees", description = "Add one or more attendees to an event. Supports adding by userId (from directory) or email. Works for both single and multiple attendees.")
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
		
		// Fetch the event to set the relationship
		Event event = eventRepository.findById(request.getEventId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
				"Event not found: " + request.getEventId()));
		
		// Map attendee info to Attendee entities, resolving userId or email
		List<Attendee> toSave = request.getAttendees().stream().map(attendeeInfo -> {
			Attendee attendee = new Attendee();
			attendee.setEvent(event);
			
			// If userId is provided, fetch user account and auto-fill info
			if (attendeeInfo.getUserId() != null) {
				UserAccount user = userAccountRepository.findById(attendeeInfo.getUserId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, 
						"User not found with userId: " + attendeeInfo.getUserId()));
				
				// Set user relationship
				attendee.setUser(user);
				
				// Auto-fill from user account (can be overridden)
				attendee.setName(attendeeInfo.getName() != null && !attendeeInfo.getName().trim().isEmpty() 
					? attendeeInfo.getName() : user.getName());
				attendee.setEmail(attendeeInfo.getEmail() != null && !attendeeInfo.getEmail().trim().isEmpty()
					? attendeeInfo.getEmail() : user.getEmail());
			} else {
				// Use email - validate that either userId or email is provided
				if (attendeeInfo.getEmail() == null || attendeeInfo.getEmail().trim().isEmpty()) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
						"Either userId or email must be provided for each attendee");
				}
				
				// Try to resolve userId from email (optional - if user exists, link them)
				Optional<UserAccount> userByEmail = userAccountRepository.findByEmailIgnoreCase(attendeeInfo.getEmail());
				if (userByEmail.isPresent()) {
					UserAccount user = userByEmail.get();
					// Link user account
					attendee.setUser(user);
					// Auto-fill name if not provided
					attendee.setName(attendeeInfo.getName() != null && !attendeeInfo.getName().trim().isEmpty()
						? attendeeInfo.getName() : user.getName());
				} else {
					// User doesn't exist - use provided info, no user link
					attendee.setUser(null);
					if (attendeeInfo.getName() == null || attendeeInfo.getName().trim().isEmpty()) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
							"Name is required when email is provided and user doesn't exist in directory");
					}
					attendee.setName(attendeeInfo.getName());
				}
				
				attendee.setEmail(attendeeInfo.getEmail());
			}
			
			return attendee;
		}).collect(Collectors.toList());
		
		List<Attendee> saved = attendeeService.addAll(toSave);
		
		// Audit log
		Map<String, Object> bulkMetadata = new HashMap<>();
		bulkMetadata.put("attendeeCount", saved.size());
		
		auditLogService.builder()
			.domain("ATTENDEE")
			.entityType("Attendee")
			.action(saved.size() == 1 ? ActionType.CREATE : ActionType.BULK_IMPORT)
			.user(principal.getUser().getId(), null, principal.getUser().getEmail())
			.description(String.format(saved.size() == 1 ? "Added attendee" : "Bulk imported %d attendees", saved.size()))
			.request(getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null)
			.eventId(request.getEventId())
			.metadata(bulkMetadata)
			.log();
		
		// Convert to DTOs
		List<AttendeeResponse> responses = attendeeService.toResponseList(saved);
		return ResponseEntity.ok(responses);
	}

	// ==================== Unified List/Filter/Get Operations ====================

	@GetMapping
	@Operation(summary = "Get, list, or filter attendees", 
		description = "Unified endpoint for retrieving attendees. Supports: single ID (?id=uuid), multiple IDs (?ids=uuid1,uuid2), event filtering (?eventId=uuid), and advanced filtering with pagination. Always returns a list.")
	@RequiresPermission(value = RbacPermissions.ATTENDEE_READ)
	public ResponseEntity<List<AttendeeResponse>> getAttendees(
			@RequestParam(required = false) @Parameter(description = "Single attendee ID") 
				String id,
			@RequestParam(required = false) @Parameter(description = "Multiple attendee IDs (comma-separated)") 
				String ids,
			@RequestParam(required = false) @Parameter(description = "Event ID for filtering") 
				String eventId,
			@RequestParam(required = false) @Parameter(description = "Filter by RSVP status (comma-separated): PENDING,CONFIRMED,DECLINED,TENTATIVE,NO_SHOW") 
				String status,
			@RequestParam(required = false) @Parameter(description = "Filter by check-in status: true (checked in) or false (not checked in)") 
				Boolean checkedIn,
			@RequestParam(required = false) @Parameter(description = "Search by name or email") 
				String search,
			@RequestParam(required = false) @Parameter(description = "Filter by user ID (from directory)") 
				String userId,
			@RequestParam(required = false) @Parameter(description = "Filter by email") 
				String email,
			@RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-indexed, only used with eventId)") 
				int page,
			@RequestParam(defaultValue = "20") @Parameter(description = "Page size (only used with eventId)") 
				int size,
			@RequestParam(defaultValue = "name") @Parameter(description = "Sort field: name, email, rsvpStatus, checkedInAt, createdAt (only used with eventId)") 
				String sortBy,
			@RequestParam(defaultValue = "ASC") @Parameter(description = "Sort direction: ASC or DESC (only used with eventId)") 
				String sortDirection,
			@AuthenticationPrincipal UserPrincipal principal) {
		try {
			List<AttendeeResponse> responses;
			
			// Priority 1: Single ID lookup
			if (StringUtils.hasText(id)) {
				UUID attendeeId = UUID.fromString(id.trim());
				Optional<Attendee> attendeeOpt = attendeeService.getById(attendeeId);
				if (attendeeOpt.isEmpty()) {
					throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found: " + id);
				}
				
				Attendee attendee = attendeeOpt.get();
				
				// Verify user can access the event
				if (attendee.getEvent() == null) {
					throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found for attendee");
				}
				UUID eventUuid = attendee.getEvent().getId();
				if (!authorizationService.canAccessEvent(principal, eventUuid)) {
					throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
						"Access denied to event: " + eventUuid);
				}
				
				responses = Collections.singletonList(attendeeService.toResponse(attendee));
				return ResponseEntity.ok(responses);
			}
			
			// Priority 2: Multiple IDs lookup
			if (StringUtils.hasText(ids)) {
				List<UUID> attendeeIds = Arrays.stream(ids.split(","))
					.map(String::trim)
					.filter(StringUtils::hasText)
					.map(UUID::fromString)
					.collect(Collectors.toList());
				
				if (attendeeIds.isEmpty()) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid attendee IDs provided");
				}
				
				List<Attendee> attendees = attendeeIds.stream()
					.map(attendeeService::getById)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.collect(Collectors.toList());
				
				// Verify access to all events
				Set<UUID> eventIds = attendees.stream()
					.map(a -> a.getEvent() != null ? a.getEvent().getId() : null)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
				
				for (UUID eventUuid : eventIds) {
					if (!authorizationService.canAccessEvent(principal, eventUuid)) {
						throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
							"Access denied to event: " + eventUuid);
					}
				}
				
				responses = attendeeService.toResponseList(attendees);
				return ResponseEntity.ok(responses);
			}
			
			// Priority 3: Event-based filtering (requires eventId)
			if (!StringUtils.hasText(eventId)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
					"Either 'id', 'ids', or 'eventId' parameter must be provided");
			}
			
			UUID eventUuid = UUID.fromString(eventId.trim());
			
			// Verify event access
			if (!authorizationService.canAccessEvent(principal, eventUuid)) {
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
				attendees = attendeeService.searchAttendees(eventUuid, search.trim(), pageable);
			}
			// Apply check-in filter if provided
			else if (checkedIn != null) {
				if (checkedIn) {
					attendees = attendeeService.listCheckedIn(eventUuid, pageable);
				} else {
					attendees = attendeeRepository.findNotCheckedInByEventId(eventUuid, pageable);
				}
			}
			// Apply status filter if provided
			else if (StringUtils.hasText(status)) {
				List<Attendee.Status> statuses = parseStatuses(status);
				if (statuses.isEmpty()) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status values");
				}
				attendees = attendeeService.listByEventAndStatuses(eventUuid, statuses, pageable);
			}
			// Apply userId filter if provided
			else if (StringUtils.hasText(userId)) {
				UUID userUuid = UUID.fromString(userId.trim());
				Optional<Attendee> attendeeOpt = attendeeRepository.findByEventIdAndUserId(eventUuid, userUuid);
				if (attendeeOpt.isPresent()) {
					attendees = new org.springframework.data.domain.PageImpl<>(
						Collections.singletonList(attendeeOpt.get()), 
						pageable, 
						1
					);
				} else {
					attendees = Page.empty(pageable);
				}
			}
			// Apply email filter if provided
			else if (StringUtils.hasText(email)) {
				Optional<Attendee> attendeeOpt = attendeeRepository.findByEventIdAndEmail(eventUuid, email.trim());
				if (attendeeOpt.isPresent()) {
					attendees = new org.springframework.data.domain.PageImpl<>(
						Collections.singletonList(attendeeOpt.get()), 
						pageable, 
						1
					);
				} else {
					attendees = Page.empty(pageable);
				}
			}
			// No filters - return all
			else {
				attendees = attendeeService.listByEventPaginated(eventUuid, pageable);
			}
			
			// Convert to DTOs
			responses = attendeeService.toResponseList(attendees.getContent());
			return ResponseEntity.ok(responses);
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameters: " + e.getMessage());
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
	private List<Attendee.Status> parseStatuses(String statusParam) {
		if (!StringUtils.hasText(statusParam)) {
			return Collections.emptyList();
		}
		
		List<Attendee.Status> statuses = new ArrayList<>();
		String[] parts = statusParam.toUpperCase().split(",");
		
		for (String part : parts) {
			String trimmed = part.trim();
			if (Attendee.Status.isValid(trimmed)) {
				statuses.add(Attendee.Status.fromString(trimmed));
			} else {
				log.warn("Invalid status value: {}", trimmed);
			}
		}
		
		return statuses;
	}
}
