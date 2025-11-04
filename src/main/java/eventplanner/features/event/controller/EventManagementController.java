package eventplanner.features.event.controller;

import eventplanner.features.event.dto.request.EventCapacityUpdateRequest;
import eventplanner.features.event.dto.request.EventDuplicateRequest;
import eventplanner.features.event.dto.request.EventRegistrationDeadlineRequest;
import eventplanner.features.event.dto.request.EventStatusUpdateRequest;
import eventplanner.features.event.dto.request.EventVisibilityUpdateRequest;
import jakarta.validation.Valid;
import eventplanner.features.event.dto.response.EventAnalyticsResponse;
import eventplanner.features.event.dto.response.EventCapacityResponse;
import eventplanner.features.event.dto.response.EventHealthCheckResponse;
import eventplanner.features.event.dto.response.EventQRCodeResponse;
import eventplanner.features.event.dto.response.EventResponse;
import eventplanner.features.event.dto.response.EventValidationResponse;
import eventplanner.features.event.dto.response.EventVisibilityResponse;
import eventplanner.features.event.dto.response.UserEventRelationshipResponse;
import eventplanner.features.event.dto.response.UserEventsSummaryResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.service.EventService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.domain.enums.EventStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive Event Management Controller with RBAC
 * Implements all 12 categories of event management endpoints
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Management", description = "Comprehensive event management operations")
@SecurityRequirement(name = "bearerAuth")
public class EventManagementController {

    private final EventService eventService;

    public EventManagementController(EventService eventService) {
        this.eventService = eventService;
    }

    // ==================== 1. USER-EVENT RELATIONSHIP ENDPOINTS ====================

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all events for a user", description = "Retrieve all events a user has a relationship with")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Events retrieved successfully",
                content = @Content(schema = @Schema(implementation = UserEventRelationshipResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<UserEventRelationshipResponse>> getUserEvents(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        try {
            List<Event> events = eventService.getEventsByUser(userId);
            List<UserEventRelationshipResponse> responses = events.stream()
                    .map(event -> convertToUserEventRelationship(event))
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/user/{userId}/owned")
    @Operation(summary = "Get events owned by user", description = "Retrieve events owned by a specific user")
    public ResponseEntity<List<UserEventRelationshipResponse>> getEventsOwnedByUser(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        try {
            List<Event> events = eventService.getEventsOwnedByUser(userId);
            List<UserEventRelationshipResponse> responses = events.stream()
                    .map(event -> convertToUserEventRelationship(event))
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/user/{userId}/upcoming")
    @Operation(summary = "Get upcoming events for user", description = "Retrieve upcoming events for a specific user")
    public ResponseEntity<List<UserEventRelationshipResponse>> getUpcomingEventsByUser(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        try {
            List<Event> events = eventService.getUpcomingEventsByUser(userId);
            List<UserEventRelationshipResponse> responses = events.stream()
                    .map(event -> convertToUserEventRelationship(event))
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/user/{userId}/past")
    @Operation(summary = "Get past events for user", description = "Retrieve past events for a specific user")
    public ResponseEntity<List<UserEventRelationshipResponse>> getPastEventsByUser(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        try {
            List<Event> events = eventService.getPastEventsByUser(userId);
            List<UserEventRelationshipResponse> responses = events.stream()
                    .map(event -> convertToUserEventRelationship(event))
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/my-events")
    @Operation(summary = "Get current user's events", description = "Retrieve all events for the current authenticated user")
    public ResponseEntity<UserEventsSummaryResponse> getMyEvents(
            @Parameter(description = "Gateway injected user identifier")
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            UUID userId = resolveUserId(userIdHeader);
            UserEventsSummaryResponse summary = eventService.getUserEventsSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/my-events/owned")
    @Operation(summary = "Get current user's owned events", description = "Retrieve events owned by the current user")
    public ResponseEntity<List<UserEventRelationshipResponse>> getMyOwnedEvents(
            @Parameter(description = "Gateway injected user identifier")
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            UUID userId = resolveUserId(userIdHeader);
            List<Event> events = eventService.getEventsOwnedByUser(userId);
            List<UserEventRelationshipResponse> responses = events.stream()
                    .map(event -> convertToUserEventRelationship(event))
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/my-events/upcoming")
    @Operation(summary = "Get current user's upcoming events", description = "Retrieve upcoming events for the current user")
    public ResponseEntity<List<UserEventRelationshipResponse>> getMyUpcomingEvents(
            @Parameter(description = "Gateway injected user identifier")
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            UUID userId = resolveUserId(userIdHeader);
            List<Event> events = eventService.getUpcomingEventsByUser(userId);
            List<UserEventRelationshipResponse> responses = events.stream()
                    .map(event -> convertToUserEventRelationship(event))
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/my-events/past")
    @Operation(summary = "Get current user's past events", description = "Retrieve past events for the current user")
    public ResponseEntity<List<UserEventRelationshipResponse>> getMyPastEvents(
            @Parameter(description = "Gateway injected user identifier")
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            UUID userId = resolveUserId(userIdHeader);
            List<Event> events = eventService.getPastEventsByUser(userId);
            List<UserEventRelationshipResponse> responses = events.stream()
                    .map(event -> convertToUserEventRelationship(event))
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== 2. EVENT STATUS & LIFECYCLE ENDPOINTS ====================

    @GetMapping("/{id}/status")
    @Operation(summary = "Get event status", description = "Retrieve the current status of an event. Only accessible if event is public, user is owner, or user has appropriate role.")
    public ResponseEntity<EventStatus> getEventStatus(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        try {
            Event event = eventService.getByIdWithAccessControl(id, user)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found or access denied"));
            return ResponseEntity.ok(event.getEventStatus());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update event status", description = "Update the status of an event")
    public ResponseEntity<EventResponse> updateEventStatus(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventStatusUpdateRequest request) {
        try {
            Event updatedEvent = eventService.updateEventStatus(id, request.getEventStatus());
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish event", description = "Publish an event to make it visible")
    public ResponseEntity<EventResponse> publishEvent(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Event updatedEvent = eventService.publishEvent(id);
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel event", description = "Cancel an event")
    public ResponseEntity<EventResponse> cancelEvent(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Event updatedEvent = eventService.cancelEvent(id);
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete event", description = "Mark an event as completed")
    public ResponseEntity<EventResponse> completeEvent(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Event updatedEvent = eventService.completeEvent(id);
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/open-registration")
    @Operation(summary = "Open registration", description = "Open registration for an event")
    public ResponseEntity<EventResponse> openRegistration(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Event updatedEvent = eventService.openRegistration(id);
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/close-registration")
    @Operation(summary = "Close registration", description = "Close registration for an event")
    public ResponseEntity<EventResponse> closeRegistration(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Event updatedEvent = eventService.closeRegistration(id);
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== 3. EVENT DISCOVERY & SEARCH ENDPOINTS ====================

    @GetMapping("/search")
    @Operation(summary = "Search events", description = "Search events with various filters")
    public ResponseEntity<List<EventResponse>> searchEvents(
            @Parameter(description = "Search term") @RequestParam(required = false) String q,
            @Parameter(description = "Event type") @RequestParam(required = false) String type,
            @Parameter(description = "Event status") @RequestParam(required = false) String status,
            @Parameter(description = "Start date from") @RequestParam(required = false) String dateFrom,
            @Parameter(description = "Start date to") @RequestParam(required = false) String dateTo) {
        try {
            List<Event> events;
            if (q != null && !q.trim().isEmpty()) {
                events = eventService.searchEvents(q);
            } else {
                events = eventService.getPublicEvents();
            }
            
            // Apply additional filters
            if (type != null) {
                events = events.stream()
                        .filter(event -> event.getEventType().toString().equals(type))
                        .toList();
            }
            if (status != null) {
                events = events.stream()
                        .filter(event -> event.getEventStatus().toString().equals(status))
                        .toList();
            }
            
            List<EventResponse> responses = events.stream()
                    .map(eventService::toResponse)
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/public")
    @Operation(summary = "Get public events", description = "Retrieve all public events")
    public ResponseEntity<List<EventResponse>> getPublicEvents() {
        try {
            List<Event> events = eventService.getPublicEvents();
            List<EventResponse> responses = events.stream()
                    .map(eventService::toResponse)
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured events", description = "Retrieve featured events")
    public ResponseEntity<List<EventResponse>> getFeaturedEvents(Pageable pageable) {
        try {
            List<Event> events = eventService.getFeaturedEvents(pageable);
            List<EventResponse> responses = events.stream()
                    .map(eventService::toResponse)
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending events", description = "Retrieve trending events")
    public ResponseEntity<List<EventResponse>> getTrendingEvents(Pageable pageable) {
        try {
            List<Event> events = eventService.getTrendingEvents(pageable);
            List<EventResponse> responses = events.stream()
                    .map(eventService::toResponse)
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming events", description = "Retrieve upcoming public events")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents() {
        try {
            List<Event> events = eventService.getUpcomingEvents();
            List<EventResponse> responses = events.stream()
                    .map(eventService::toResponse)
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/by-type/{type}")
    @Operation(summary = "Get events by type", description = "Retrieve events by event type")
    public ResponseEntity<List<EventResponse>> getEventsByType(
            @Parameter(description = "Event type") @PathVariable String type) {
        try {
            List<Event> events = eventService.getEventsByType(type);
            List<EventResponse> responses = events.stream()
                    .map(eventService::toResponse)
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/by-status/{status}")
    @Operation(summary = "Get events by status", description = "Retrieve events by event status")
    public ResponseEntity<List<EventResponse>> getEventsByStatus(
            @Parameter(description = "Event status") @PathVariable String status) {
        try {
            List<Event> events = eventService.getEventsByStatus(status);
            List<EventResponse> responses = events.stream()
                    .map(eventService::toResponse)
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== 4. EVENT CAPACITY & REGISTRATION ENDPOINTS ====================

    @GetMapping("/{id}/capacity")
    @Operation(summary = "Get event capacity", description = "Retrieve capacity information for an event. Only accessible if event is public, user is owner, or user has appropriate role.")
    public ResponseEntity<EventCapacityResponse> getEventCapacity(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        try {
            Event event = eventService.getByIdWithAccessControl(id, user)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found or access denied"));
            
            EventCapacityResponse response = new EventCapacityResponse();
            response.setEventId(id);
            response.setCapacity(event.getCapacity());
            response.setCurrentAttendeeCount(event.getCurrentAttendeeCount());
            response.setAvailableSpots(eventService.getAvailableCapacity(id));
            response.setUtilizationPercentage(calculateUtilizationPercentage(event.getCapacity(), event.getCurrentAttendeeCount()));
            response.setIsRegistrationOpen(event.getEventStatus() == EventStatus.REGISTRATION_OPEN && 
                (event.getRegistrationDeadline() == null || LocalDateTime.now().isBefore(event.getRegistrationDeadline())));
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/{id}/capacity")
    @Operation(summary = "Update event capacity", description = "Update the capacity of an event")
    public ResponseEntity<EventResponse> updateCapacity(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventCapacityUpdateRequest request) {
        try {
            Event updatedEvent = eventService.updateCapacity(id, request.getCapacity());
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}/capacity/available")
    @Operation(summary = "Get available capacity", description = "Get the number of available spots for an event")
    public ResponseEntity<Integer> getAvailableCapacity(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Integer available = eventService.getAvailableCapacity(id);
            return ResponseEntity.ok(available);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/{id}/registration-deadline")
    @Operation(summary = "Update registration deadline", description = "Update the registration deadline for an event")
    public ResponseEntity<EventResponse> updateRegistrationDeadline(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventRegistrationDeadlineRequest request) {
        try {
            Event updatedEvent = eventService.updateRegistrationDeadline(id, request.getDeadline());
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== 5. EVENT QR CODE ENDPOINTS ====================

    @GetMapping("/{id}/qr-code")
    @Operation(summary = "Get event QR code", description = "Retrieve the QR code for an event. Only accessible if event is public, user is owner, or user has appropriate role.")
    public ResponseEntity<EventQRCodeResponse> getQRCode(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        try {
            Event event = eventService.getByIdWithAccessControl(id, user)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found or access denied"));
            
            EventQRCodeResponse response = new EventQRCodeResponse();
            response.setEventId(id);
            response.setQrCode(event.getQrCode());
            response.setQrCodeEnabled(event.getQrCodeEnabled());
            response.setQrCodeImageUrl(event.getQrCode() != null ? "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + event.getQrCode() : null);
            response.setGeneratedAt(event.getUpdatedAt() != null ? event.getUpdatedAt().toString() : null);
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/qr-code/generate")
    @Operation(summary = "Generate QR code", description = "Generate a QR code for an event")
    public ResponseEntity<EventResponse> generateQRCode(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Event updatedEvent = eventService.generateQRCode(id);
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/qr-code/regenerate")
    @Operation(summary = "Regenerate QR code", description = "Regenerate the QR code for an event")
    public ResponseEntity<EventResponse> regenerateQRCode(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Event updatedEvent = eventService.regenerateQRCode(id);
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/{id}/qr-code")
    @Operation(summary = "Disable QR code", description = "Disable the QR code for an event")
    public ResponseEntity<EventResponse> disableQRCode(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Event updatedEvent = eventService.disableQRCode(id);
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== 6. EVENT VISIBILITY & ACCESS CONTROL ENDPOINTS ====================

    @GetMapping("/{id}/visibility")
    @Operation(summary = "Get event visibility", description = "Get the visibility settings for an event. Only accessible if event is public, user is owner, or user has appropriate role.")
    public ResponseEntity<EventVisibilityResponse> getVisibility(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        try {
            Event event = eventService.getByIdWithAccessControl(id, user)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found or access denied"));
            
            EventVisibilityResponse response = new EventVisibilityResponse();
            response.setEventId(id);
            response.setIsPublic(event.getIsPublic());
            response.setRequiresApproval(event.getRequiresApproval());
            response.setAccessLevel(event.getIsPublic() ? "public" : "private");
            response.setUpdatedAt(event.getUpdatedAt() != null ? event.getUpdatedAt().toString() : null);
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/{id}/visibility")
    @Operation(summary = "Update event visibility", description = "Update the visibility settings for an event")
    public ResponseEntity<EventResponse> updateVisibility(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventVisibilityUpdateRequest request) {
        try {
            Event updatedEvent = eventService.updateVisibility(id, request.getIsPublic());
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/make-public")
    @Operation(summary = "Make event public", description = "Make an event public")
    public ResponseEntity<EventResponse> makeEventPublic(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Event updatedEvent = eventService.makeEventPublic(id);
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/make-private")
    @Operation(summary = "Make event private", description = "Make an event private")
    public ResponseEntity<EventResponse> makeEventPrivate(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Event updatedEvent = eventService.makeEventPrivate(id);
            return ResponseEntity.ok(eventService.toResponse(updatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== 7. EVENT ANALYTICS ENDPOINTS ====================

    @GetMapping("/{id}/analytics")
    @Operation(summary = "Get event analytics", description = "Get comprehensive analytics for an event")
    public ResponseEntity<EventAnalyticsResponse> getEventAnalytics(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Map<String, Object> analytics = eventService.getEventAnalytics(id);
            
            EventAnalyticsResponse response = new EventAnalyticsResponse();
            response.setEventId(id);
            response.setTotalViews((Long) analytics.getOrDefault("totalViews", 0L));
            response.setUniqueVisitors((Long) analytics.getOrDefault("uniqueVisitors", 0L));
            response.setRegistrationRate((Double) analytics.getOrDefault("registrationRate", 0.0));
            response.setAttendanceRate((Double) analytics.getOrDefault("attendanceRate", 0.0));
            response.setEngagementMetrics(safeCastToMap(analytics.getOrDefault("engagementMetrics", Map.<String, Object>of())));
            response.setSocialMetrics(safeCastToMap(analytics.getOrDefault("socialMetrics", Map.<String, Object>of())));
            response.setGeographicDistribution(safeCastToMap(analytics.getOrDefault("geographicDistribution", Map.<String, Object>of())));
            response.setAnalyticsPeriod("30d");
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== 8. EVENT DUPLICATION & TEMPLATES ENDPOINTS ====================

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Duplicate event", description = "Create a duplicate of an existing event")
    public ResponseEntity<EventResponse> duplicateEvent(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventDuplicateRequest request) {
        try {
            Event duplicatedEvent = eventService.duplicateEvent(id, request.getNewEventName());
            return ResponseEntity.ok(eventService.toResponse(duplicatedEvent));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== 9. EVENT VALIDATION & HEALTH CHECK ENDPOINTS ====================

    @GetMapping("/{id}/validation")
    @Operation(summary = "Validate event", description = "Validate event data and return validation results")
    public ResponseEntity<EventValidationResponse> validateEvent(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Map<String, Object> validation = eventService.validateEvent(id);
            
            EventValidationResponse response = new EventValidationResponse();
            response.setEventId(id);
            response.setIsValid((Boolean) validation.getOrDefault("isValid", false));
            response.setValidationScore((Integer) validation.getOrDefault("score", 0));
            response.setErrors(safeCastToList(validation.getOrDefault("errors", List.<String>of())));
            response.setWarnings(safeCastToList(validation.getOrDefault("warnings", List.<String>of())));
            response.setValidationDetails(safeCastToMap(validation.getOrDefault("details", Map.<String, Object>of())));
            response.setValidatedAt(java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}/health")
    @Operation(summary = "Event health check", description = "Perform a health check on an event")
    public ResponseEntity<EventHealthCheckResponse> getEventHealthCheck(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Map<String, Object> healthCheck = eventService.getEventHealthCheck(id);
            
            EventHealthCheckResponse response = new EventHealthCheckResponse();
            response.setEventId(id);
            response.setHealthStatus((String) healthCheck.getOrDefault("status", "unknown"));
            response.setHealthScore((Integer) healthCheck.getOrDefault("score", 0));
            response.setIssues(safeCastToList(healthCheck.getOrDefault("issues", List.<String>of())));
            response.setRecommendations(safeCastToList(healthCheck.getOrDefault("recommendations", List.<String>of())));
            response.setComponentHealth(safeCastToMap(healthCheck.getOrDefault("components", Map.<String, Object>of())));
            response.setCheckedAt(java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== HELPER METHODS ====================

    private UUID resolveUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user ID format: " + userIdHeader);
        }
    }

    private UserEventRelationshipResponse convertToUserEventRelationship(Event event) {
        UserEventRelationshipResponse response = new UserEventRelationshipResponse();
        response.setEventId(event.getId());
        response.setEventName(event.getName());
        response.setEventDescription(event.getDescription());
        response.setEventType(event.getEventType());
        response.setEventStatus(event.getEventStatus());
        response.setStartDateTime(event.getStartDateTime());
        response.setEndDateTime(event.getEndDateTime());
        response.setIsOwner(true); // For owned events
        response.setCapacity(event.getCapacity());
        response.setCurrentAttendeeCount(event.getCurrentAttendeeCount());
        response.setIsPublic(event.getIsPublic());
        response.setCoverImageUrl(event.getCoverImageUrl());
        response.setEventWebsiteUrl(event.getEventWebsiteUrl());
        response.setHashtag(event.getHashtag());
        return response;
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeCastToMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> safeCastToList(Object obj) {
        if (obj instanceof List) {
            return (List<String>) obj;
        }
        return List.of();
    }
}
