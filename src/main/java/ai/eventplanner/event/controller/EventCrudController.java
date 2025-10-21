package ai.eventplanner.event.controller;

import ai.eventplanner.event.dto.request.CreateEventRequest;
import ai.eventplanner.event.dto.request.UpdateEventRequest;
import ai.eventplanner.event.dto.response.EventResponse;
import ai.eventplanner.event.entity.Event;
import ai.eventplanner.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events CRUD", description = "Basic CRUD operations for events")
@SecurityRequirement(name = "bearerAuth")
public class EventCrudController {

    private final EventService eventService;

    public EventCrudController(EventService eventService) {
        this.eventService = eventService;
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Retrieve a specific event by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event found",
                content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing Bearer token")
    })
    public ResponseEntity<EventResponse> get(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        Optional<Event> found = eventService.getById(id);
        return found
                .map(eventService::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new event", description = "Create a new event with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Event created successfully",
                content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing Bearer token")
    })
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody CreateEventRequest request,
            @Parameter(description = "Gateway injected user identifier")
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            UUID ownerId = resolveOwnerId(userIdHeader);
            Event created = eventService.create(request, ownerId);
            EventResponse response = eventService.toResponse(created);
            return ResponseEntity.created(URI.create("/api/v1/events/" + response.getId())).body(response);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update event", description = "Update an existing event with new details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event updated successfully",
                content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing Bearer token")
    })
    public ResponseEntity<EventResponse> update(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request) {
        try {
            Optional<Event> existingEvent = eventService.getById(id);
            if (existingEvent.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Event updated = eventService.update(id, request);
            EventResponse response = eventService.toResponse(updated);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete event", description = "Delete an event by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing Bearer token")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveOwnerId(String userIdHeader) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                return UUID.fromString(userIdHeader);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-User-Id header. Must be a UUID.", ex);
            }
        }
        return null;
    }
}
