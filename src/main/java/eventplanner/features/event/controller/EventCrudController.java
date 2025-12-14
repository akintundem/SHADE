package eventplanner.features.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.features.event.dto.request.CreateEventWithCoverUploadRequest;
import eventplanner.features.event.dto.request.EventListRequest;
import eventplanner.features.event.dto.request.UpdateEventWithCoverUploadRequest;
import eventplanner.features.event.dto.request.EventFeedRequest;
import eventplanner.features.event.dto.response.CreateEventWithCoverUploadResponse;
import eventplanner.features.event.dto.response.EventCapacityResponse;
import eventplanner.features.event.dto.response.EventVisibilityResponse;
import eventplanner.features.event.dto.response.EventResponse;
import eventplanner.features.event.dto.response.EventFeedResponse;
import eventplanner.features.event.dto.response.UpdateEventWithCoverUploadResponse;
import eventplanner.common.domain.enums.EventScope;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.service.EventIdempotencyService;
import eventplanner.features.event.service.EventMediaService;
import eventplanner.features.event.service.EventService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events CRUD", description = "Basic CRUD operations for events")
@SecurityRequirement(name = "bearerAuth")
public class EventCrudController {

    private final EventService eventService;
    private final EventMediaService eventMediaService;
    private final AuthorizationService authorizationService;
    private final EventIdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public EventCrudController(EventService eventService, 
                              EventMediaService eventMediaService,
                              AuthorizationService authorizationService,
                              EventIdempotencyService idempotencyService,
                              ObjectMapper objectMapper) {
        this.eventService = eventService;
        this.eventMediaService = eventMediaService;
        this.authorizationService = authorizationService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }


    @GetMapping(params = "!mine")
    @RequiresPermission(RbacPermissions.PUBLIC_EVENTS_SEARCH)
    @Operation(summary = "List events", description = "COMPLETE - List events with pagination, filtering, and search. Supports filtering by status, visibility, date range, and search by name/tag.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<EventResponse>> list(
            @Valid EventListRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        try {
            Page<Event> events = eventService.listEvents(request, user);
            Page<EventResponse> responses = events.map(eventService::toResponse);
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping(params = "mine=true")
    @RequiresPermission(RbacPermissions.MY_EVENTS_SEARCH)
    @Operation(summary = "List my events", description = " COMPLETE - List events owned by the current user. Use timeframe=UPCOMING|PAST for convenience.")
    public ResponseEntity<Page<EventResponse>> listMine(
            @Valid EventListRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        try {
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }
            request.setMine(true);
            Page<Event> events = eventService.listEvents(request, user);
            Page<EventResponse> responses = events.map(eventService::toResponse);
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}")
    @RequiresPermission(RbacPermissions.PUBLIC_EVENTS_SEARCH)
    @Operation(summary = "Get event by ID", description = "COMPLETE - Retrieve a specific event by its unique identifier. Returns full details for owners/high-responsibility users, or feed view for guests.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event found",
                content = @Content(schema = @Schema(oneOf = {EventResponse.class, EventFeedResponse.class, EventCapacityResponse.class, EventVisibilityResponse.class}))),
        @ApiResponse(responseCode = "404", description = "Event not found or access denied"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing Bearer token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<?> get(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "Optional response projection. Supported: capacity, visibility, full, feed. Default: full for owners/high-responsibility; feed for guests.") 
            @RequestParam(value = "view", required = false) String view,
            @Parameter(description = "Feed pagination (only used for guest users)") 
            @ModelAttribute @Valid EventFeedRequest feedRequest) {
        Optional<Event> found = eventService.getByIdWithAccessControl(id, user);
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Event event = found.get();
        EventScope scope = eventService.determineEventScope(user, id);

        // Normalize requested view
        String normalizedView = view != null ? view.trim().toLowerCase(java.util.Locale.ROOT) : null;
        if (normalizedView != null && normalizedView.isEmpty()) {
            normalizedView = null;
        }

        // Enforce scope: guests cannot request "full"
        if (scope != EventScope.FULL && "full".equals(normalizedView)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Projection: capacity
        if ("capacity".equals(normalizedView)) {
            EventCapacityResponse response = new EventCapacityResponse();
            response.setEventId(id);
            response.setCapacity(event.getCapacity());
            response.setCurrentAttendeeCount(event.getCurrentAttendeeCount());
            response.setAvailableSpots(eventService.getAvailableCapacity(id));
            response.setUtilizationPercentage(calculateUtilizationPercentage(event.getCapacity(), event.getCurrentAttendeeCount()));
            boolean open = event.getEventStatus() == eventplanner.common.domain.enums.EventStatus.REGISTRATION_OPEN
                    && (event.getRegistrationDeadline() == null || LocalDateTime.now(ZoneOffset.UTC).isBefore(event.getRegistrationDeadline()));
            response.setIsRegistrationOpen(open);
            return ResponseEntity.ok(response);
        }

        // Projection: visibility (includes registration deadline)
        if ("visibility".equals(normalizedView)) {
            EventVisibilityResponse response = new EventVisibilityResponse();
            response.setEventId(id);
            response.setIsPublic(event.getIsPublic());
            response.setRequiresApproval(event.getRequiresApproval());
            response.setRegistrationDeadline(event.getRegistrationDeadline());
            response.setAccessLevel(Boolean.TRUE.equals(event.getIsPublic()) ? "public" : "private");
            response.setUpdatedAt(event.getUpdatedAt() != null ? event.getUpdatedAt().toString() : null);
            return ResponseEntity.ok(response);
        }
        
        // Default behavior (or explicit full/feed)
        if (scope == EventScope.FULL && !"feed".equals(normalizedView)) {
            // Return full event details for owners/high-responsibility users
            EventResponse response = eventService.toResponse(event);
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
            if (event.getVersion() != null) {
                builder.eTag(String.valueOf(event.getVersion()));
            }
            return builder.body(response);
        } else {
            // Return feed view for guests/low-responsibility users (with pagination)
            EventFeedRequest request = feedRequest != null ? feedRequest : new EventFeedRequest();
            EventFeedResponse feedResponse = eventService.toFeedResponse(event, user, request);
            return ResponseEntity.ok(feedResponse);
        }
    }

    private Double calculateUtilizationPercentage(Integer capacity, Integer currentAttendeeCount) {
        if (capacity == null || capacity == 0) {
            return 0.0;
        }
        if (currentAttendeeCount == null) {
            return 0.0;
        }
        return (double) (currentAttendeeCount * 100) / capacity;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequiresPermission(RbacPermissions.EVENT_CREATE)
    @Operation(
            summary = "Create new event",
            description = "COMPLETE - Create a new event and return a presigned URL for uploading the cover image. "
                    + "Client uploads to S3, then calls the cover-image complete endpoint to persist coverImageUrl. "
                    + "Supports Idempotency-Key header to prevent duplicate creation on retries."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Event created successfully with cover upload details",
                content = @Content(schema = @Schema(implementation = CreateEventWithCoverUploadResponse.class))),
        @ApiResponse(responseCode = "200", description = "Event already exists (idempotent replay)",
                content = @Content(schema = @Schema(implementation = CreateEventWithCoverUploadResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing Bearer token"),
        @ApiResponse(responseCode = "409", description = "Conflict - Operation already in progress")
    })
    public ResponseEntity<CreateEventWithCoverUploadResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateEventWithCoverUploadRequest request) {
        try {
            if (principal == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }

            // Enforce one-flow: cover image must be uploaded via S3 presigned flow, not by arbitrary URL
            if (request.getEvent() != null && StringUtils.hasText(request.getEvent().getCoverImageUrl())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Do not set coverImageUrl on create. Use the presigned cover upload flow and then call the cover-image complete endpoint.");
            }
            
            // Check idempotency - return cached result if available
            if (StringUtils.hasText(idempotencyKey)) {
                Optional<String> cachedResult = idempotencyService.getProcessedResult(idempotencyKey);
                if (cachedResult.isPresent()) {
                    try {
                        CreateEventWithCoverUploadResponse cached = objectMapper.readValue(cachedResult.get(), CreateEventWithCoverUploadResponse.class);
                        return ResponseEntity.ok()
                                .header("X-Idempotency-Replay", "true")
                                .header(HttpHeaders.LOCATION, "/api/v1/events/" + cached.getEvent().getId())
                                .body(cached);
                    } catch (Exception e) {
                        // Log but continue with normal creation
                    }
                }
                
                // Mark as processing to prevent concurrent requests
                if (!idempotencyService.markAsProcessing(idempotencyKey)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, 
                            "Event creation already in progress. Please wait.");
                }
            }
            
            try {
                Event created = eventService.create(request.getEvent(), principal.getId());
                EventResponse eventResponse = eventService.toResponse(created);
                var coverUpload = eventMediaService.createCoverImageUpload(created.getId(), principal, request.getCoverUpload());

                CreateEventWithCoverUploadResponse response = CreateEventWithCoverUploadResponse.builder()
                        .event(eventResponse)
                        .coverUpload(coverUpload)
                        .build();
                
                // Store result for idempotency
                if (StringUtils.hasText(idempotencyKey)) {
                    try {
                        String resultJson = objectMapper.writeValueAsString(response);
                        idempotencyService.storeResult(idempotencyKey, resultJson);
                    } catch (Exception e) {
                        // Log but don't fail the request
                    }
                }
                
                URI location = URI.create("/api/v1/events/" + created.getId());
                return ResponseEntity.created(location)
                        .header(HttpHeaders.LOCATION, location.toString())
                        .body(response);
            } finally {
                // Release processing lock
                if (StringUtils.hasText(idempotencyKey)) {
                    idempotencyService.releaseProcessingLock(idempotencyKey);
                }
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#id"})
    @Operation(summary = "Update event", description = "COMPLETE - Update an existing event with new details. Supports If-Match header for optimistic locking.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event updated successfully",
                content = @Content(schema = @Schema(implementation = UpdateEventWithCoverUploadResponse.class))),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing Bearer token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @ApiResponse(responseCode = "409", description = "Conflict - Event version mismatch (optimistic locking)")
    })
    public ResponseEntity<UpdateEventWithCoverUploadResponse> update(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody UpdateEventWithCoverUploadRequest request) {
        try {
            if (principal == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }
            
            Optional<Event> existingEvent = eventService.getById(id);
            if (existingEvent.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Verify ownership or admin access
            if (!authorizationService.isEventOwner(principal, id) && !authorizationService.isAdmin(principal)) {
                throw new AccessDeniedException("Only event owners or admins can update events");
            }
            
            // Parse version from If-Match header
            Long expectedVersion = null;
            if (StringUtils.hasText(ifMatch)) {
                try {
                    expectedVersion = Long.parseLong(ifMatch);
                } catch (NumberFormatException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid If-Match header format");
                }
            }

            if (request.getEvent() != null && StringUtils.hasText(request.getEvent().getCoverImageUrl())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Do not set coverImageUrl on update. Use the presigned cover upload flow (coverUpload) and then call the cover-image complete endpoint.");
            }
            
            Event updated;
            if (expectedVersion != null) {
                updated = eventService.updateWithVersion(id, request.getEvent(), expectedVersion);
            } else {
                updated = eventService.update(id, request.getEvent());
            }

            var coverUpload = request.getCoverUpload() != null
                    ? eventMediaService.createCoverImageUpload(id, principal, request.getCoverUpload())
                    : null;

            UpdateEventWithCoverUploadResponse response = UpdateEventWithCoverUploadResponse.builder()
                    .event(eventService.toResponse(updated))
                    .coverUpload(coverUpload)
                    .build();

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
            // Include ETag with new version
            if (updated.getVersion() != null) {
                builder.eTag(String.valueOf(updated.getVersion()));
            }
            return builder.body(response);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Event has been modified by another user. Please refresh and try again.", ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/registration")
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#id"})
    @Operation(summary = "Update registration state", description = "Open or close registration for an event. Use action=open or action=close.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registration state updated successfully",
                content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<EventResponse> updateRegistrationState(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("action") String action) {
        try {
            if (principal == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }
            if (!StringUtils.hasText(action)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action is required (open|close)");
            }

            // Verify ownership or admin access
            if (!authorizationService.isEventOwner(principal, id) && !authorizationService.isAdmin(principal)) {
                throw new AccessDeniedException("Only event owners or admins can update registration state");
            }

            String normalized = action.trim().toLowerCase(java.util.Locale.ROOT);
            Event updatedEvent;
            if ("open".equals(normalized)) {
                updatedEvent = eventService.openRegistration(id);
            } else if ("close".equals(normalized)) {
                updatedEvent = eventService.closeRegistration(id);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action. Use open or close.");
            }

            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission(value = RbacPermissions.EVENT_DELETE, resources = {"event_id=#id"})
    @Operation(summary = "Archive event", description = "Archive an event (soft delete) with audit metadata. Use this instead of DELETE for recoverable removal.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event archived successfully",
                content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "400", description = "Event already archived"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<EventResponse> archive(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String reason) {
        try {
            if (principal == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }
            
            Optional<Event> existingEvent = eventService.getById(id);
            if (existingEvent.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Verify ownership or admin access
            if (!authorizationService.isEventOwner(principal, id) && !authorizationService.isAdmin(principal)) {
                throw new AccessDeniedException("Only event owners or admins can archive events");
            }
            
            Event archived = eventService.archiveEvent(id, principal.getId(), reason);
            EventResponse response = eventService.toResponse(archived);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/restore")
    @RequiresPermission(value = RbacPermissions.EVENT_DELETE, resources = {"event_id=#id"})
    @Operation(summary = "Restore archived event", description = "Restore a previously archived event.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event restored successfully",
                content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "400", description = "Event is not archived"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<EventResponse> restore(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            if (principal == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }
            
            Optional<Event> existingEvent = eventService.getById(id);
            if (existingEvent.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Verify ownership or admin access
            if (!authorizationService.isEventOwner(principal, id) && !authorizationService.isAdmin(principal)) {
                throw new AccessDeniedException("Only event owners or admins can restore events");
            }
            
            Event restored = eventService.restoreEvent(id, principal.getId());
            EventResponse response = eventService.toResponse(restored);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}/feed")
    @RequiresPermission(RbacPermissions.PUBLIC_EVENTS_SEARCH)
    @Operation(summary = "Get event feed", description = "COMPLETE - Get the social feed for an event (videos, pictures, tweets) with pagination. Available to all users with event access. Use page parameter to load more posts.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event feed retrieved successfully",
                content = @Content(schema = @Schema(implementation = EventFeedResponse.class))),
        @ApiResponse(responseCode = "404", description = "Event not found or access denied"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<EventFeedResponse> getFeed(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user,
            @ModelAttribute @Valid EventFeedRequest request) {
        EventFeedRequest feedRequest = request != null ? request : new EventFeedRequest();
        EventFeedResponse feedResponse = eventService.buildEventFeed(id, user, feedRequest);
        return ResponseEntity.ok(feedResponse);
    }

}
