package ai.eventplanner.event.controller;

import ai.eventplanner.event.dto.EventCreateRequest;
import ai.eventplanner.event.model.EventEntity;
import ai.eventplanner.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.io.IOException;
import java.util.Map;
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
                content = @Content(schema = @Schema(implementation = EventEntity.class))),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing Bearer token")
    })
    public ResponseEntity<EventEntity> get(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        Optional<EventEntity> found = eventService.getById(id);
        return found.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new event", description = "Create a new event with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Event created successfully",
                content = @Content(schema = @Schema(implementation = EventEntity.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing Bearer token")
    })
    public ResponseEntity<EventEntity> create(
            @Valid @RequestBody EventCreateRequest req,
            @Parameter(description = "Authorization header with Bearer token")
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "Gateway injected user identifier")
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        String userId = extractUserIdFromHeaders(userIdHeader, authHeader);

        EventEntity entity = new EventEntity();
        // id and status will be set in prePersist
        if (userId != null && !userId.isBlank()) {
            entity.setOrganizerId(UUID.fromString(userId));
        } else {
            entity.setOrganizerId(UUID.randomUUID());
        }
        entity.setOrganizationId(req.getOrganizationId());
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setDate(req.getDate());
        entity.setVenueId(req.getVenueId());
        entity.setMetadata(req.getMetadata() != null ? req.getMetadata() : Map.of());

        EventEntity created = eventService.create(entity);
        return ResponseEntity.created(URI.create("/api/v1/events/" + created.getId())).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete event", description = "Delete an event by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing Bearer token")
    })
    public void delete(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            HttpServletResponse response) throws IOException {
        eventService.delete(id);
        response.setStatus(HttpStatus.NO_CONTENT.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{}");
        response.getWriter().flush();
    }
    
    private String extractUserIdFromHeaders(String userIdHeader, String authHeader) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return userIdHeader;
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return null; // Gateway already validated token; no fallback user id available.
        }
        return null;
    }
}
