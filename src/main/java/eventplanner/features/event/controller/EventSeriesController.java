package eventplanner.features.event.controller;

import eventplanner.features.event.dto.request.CreateEventSeriesRequest;
import eventplanner.features.event.dto.request.GenerateOccurrencesRequest;
import eventplanner.features.event.dto.request.UpdateEventSeriesRequest;
import eventplanner.features.event.dto.response.EventResponse;
import eventplanner.features.event.dto.response.EventSeriesResponse;
import eventplanner.features.event.dto.response.GenerateOccurrencesResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.service.EventSeriesService;
import eventplanner.features.event.service.EventService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for managing event series (recurring events).
 */
@RestController
@RequestMapping("/api/v1/event-series")
@Tag(name = "Event Series", description = "Manage recurring event series")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class EventSeriesController {

    private final EventSeriesService seriesService;
    private final EventService eventService;

    // ==================== CREATE ====================

    @PostMapping
    @RequiresPermission(RbacPermissions.EVENT_CREATE)
    @Operation(summary = "Create event series", 
            description = "Create a new recurring event series with initial occurrences")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Series created successfully",
                content = @Content(schema = @Schema(implementation = EventSeriesResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<EventSeriesResponse> createSeries(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateEventSeriesRequest request) {            if (principal == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }

            EventSeriesResponse response = seriesService.createSeries(request, principal);
            URI location = URI.create("/api/v1/event-series/" + response.getId());
            return ResponseEntity.created(location).body(response);

    }

    // ==================== READ ====================

    @GetMapping("/{id}")
    @RequiresPermission(RbacPermissions.PUBLIC_EVENTS_SEARCH)
    @Operation(summary = "Get event series", description = "Get details of an event series by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Series found",
                content = @Content(schema = @Schema(implementation = EventSeriesResponse.class))),
        @ApiResponse(responseCode = "404", description = "Series not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<EventSeriesResponse> getSeries(
            @Parameter(description = "Series ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        EventSeriesResponse response = seriesService.getSeriesById(id, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @RequiresPermission(RbacPermissions.MY_EVENTS_SEARCH)
    @Operation(summary = "List my event series", description = "List event series owned by the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Series list retrieved successfully")
    })
    public ResponseEntity<Page<EventSeriesResponse>> listMySeries(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter to active series only") @RequestParam(required = false) Boolean activeOnly) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Page<EventSeriesResponse> series = seriesService.listMySeries(principal, page, size, activeOnly);
        return ResponseEntity.ok(series);
    }

    @GetMapping("/{id}/events")
    @RequiresPermission(RbacPermissions.PUBLIC_EVENTS_SEARCH)
    @Operation(summary = "List series events", description = "List all events in a series")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Events list retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Series not found")
    })
    public ResponseEntity<Page<EventResponse>> listSeriesEvents(
            @Parameter(description = "Series ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter to upcoming events only") @RequestParam(required = false) Boolean upcomingOnly) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Page<Event> events = seriesService.listSeriesEvents(id, principal, page, size, upcomingOnly);
        Page<EventResponse> responses = events.map(event -> eventService.toResponse(event, principal));
        return ResponseEntity.ok(responses);
    }

    // ==================== UPDATE ====================

    @PutMapping("/{id}")
    @RequiresPermission(RbacPermissions.EVENT_UPDATE)
    @Operation(summary = "Update event series", 
            description = "Update series settings. Can optionally cascade changes to future events.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Series updated successfully",
                content = @Content(schema = @Schema(implementation = EventSeriesResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Series not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<EventSeriesResponse> updateSeries(
            @Parameter(description = "Series ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateEventSeriesRequest request) {            if (principal == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }

            EventSeriesResponse response = seriesService.updateSeries(id, request, principal);
            return ResponseEntity.ok(response);

    }

    // ==================== GENERATE OCCURRENCES ====================

    @PostMapping("/{id}/generate")
    @RequiresPermission(RbacPermissions.EVENT_CREATE)
    @Operation(summary = "Generate occurrences", 
            description = "Manually generate additional occurrences for a series")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Occurrences generated successfully",
                content = @Content(schema = @Schema(implementation = GenerateOccurrencesResponse.class))),
        @ApiResponse(responseCode = "400", description = "Cannot generate more occurrences"),
        @ApiResponse(responseCode = "404", description = "Series not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<GenerateOccurrencesResponse> generateOccurrences(
            @Parameter(description = "Series ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) GenerateOccurrencesRequest request) {            if (principal == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }

            GenerateOccurrencesRequest req = request != null ? request : new GenerateOccurrencesRequest();
            GenerateOccurrencesResponse response = seriesService.generateOccurrences(id, req, principal);
            return ResponseEntity.ok(response);

    }

    // ==================== CANCEL / DELETE ====================

    @PostMapping("/{id}/cancel")
    @RequiresPermission(RbacPermissions.EVENT_DELETE)
    @Operation(summary = "Cancel event series", 
            description = "Deactivate a series and optionally cancel all future events")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Series cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Series not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> cancelSeries(
            @Parameter(description = "Series ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Whether to also cancel future events") 
            @RequestParam(defaultValue = "true") boolean cancelFutureEvents) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        seriesService.cancelSeries(id, principal, cancelFutureEvents);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(RbacPermissions.EVENT_DELETE)
    @Operation(summary = "Delete event series", 
            description = "Delete a series. Events can be kept (detached) or deleted.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Series deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Series not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteSeries(
            @Parameter(description = "Series ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Whether to also delete events in the series") 
            @RequestParam(defaultValue = "false") boolean deleteEvents) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        seriesService.deleteSeries(id, principal, deleteEvents);
        return ResponseEntity.noContent().build();
    }
}
