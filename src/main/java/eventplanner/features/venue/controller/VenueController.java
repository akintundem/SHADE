package eventplanner.features.venue.controller;

import eventplanner.features.venue.dto.VenueRequest;
import eventplanner.features.venue.dto.VenueResponse;
import eventplanner.features.venue.dto.VenueSearchRequest;
import eventplanner.features.venue.entity.Venue;
import eventplanner.features.venue.service.VenueService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for venue management
 */
@RestController
@RequestMapping("/api/v1/venues")
@Tag(name = "Venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    @RequiresPermission(RbacPermissions.VENUE_CREATE)
    @Operation(summary = "Create venue", description = "Create a new venue")
    public ResponseEntity<VenueResponse> createVenue(
        @Valid @RequestBody VenueRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        Venue venue = toEntity(request);
        Venue created = venueService.createVenue(venue);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{venueId}")
    @RequiresPermission(value = RbacPermissions.VENUE_UPDATE, resources = {"venue_id=#venueId"})
    @Operation(summary = "Update venue", description = "Update an existing venue")
    public ResponseEntity<VenueResponse> updateVenue(
        @PathVariable UUID venueId,
        @Valid @RequestBody VenueRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        Venue venue = toEntity(request);
        Venue updated = venueService.updateVenue(venueId, venue);
        return ResponseEntity.ok(toResponse(updated));
    }

    @GetMapping("/{venueId}")
    @RequiresPermission(RbacPermissions.VENUE_READ)
    @Operation(summary = "Get venue by ID", description = "Get venue details by ID")
    public ResponseEntity<VenueResponse> getVenue(@PathVariable UUID venueId) {
        Venue venue = venueService.getVenueById(venueId);
        return ResponseEntity.ok(toResponse(venue));
    }

    @GetMapping
    @RequiresPermission(RbacPermissions.VENUE_READ)
    @Operation(summary = "Get all venues", description = "Get all venues with pagination")
    public ResponseEntity<Page<VenueResponse>> getAllVenues(Pageable pageable) {
        Page<Venue> venues = venueService.getAllVenues(pageable);
        Page<VenueResponse> responses = venues.map(this::toResponse);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/search")
    @RequiresPermission(RbacPermissions.VENUE_READ)
    @Operation(summary = "Search venues", description = "Search venues by various criteria including location")
    public ResponseEntity<List<VenueResponse>> searchVenues(
        @RequestBody VenueSearchRequest request
    ) {
        List<Venue> venues;

        // Geo-spatial search (bounding box)
        if (request.getMinLatitude() != null && request.getMaxLatitude() != null &&
            request.getMinLongitude() != null && request.getMaxLongitude() != null) {

            venues = venueService.findVenuesWithinBounds(
                request.getMinLatitude(),
                request.getMaxLatitude(),
                request.getMinLongitude(),
                request.getMaxLongitude()
            );
        }
        // Geo-spatial search (radius)
        else if (request.getLatitude() != null && request.getLongitude() != null) {
            venues = venueService.findVenuesNearLocation(
                request.getLatitude(),
                request.getLongitude(),
                request.getRadiusDegrees()
            );
        }
        // Search by city and state with capacity filter
        else if (request.getCity() != null && request.getState() != null && request.getMinCapacity() != null) {
            venues = venueService.findVenuesWithMinCapacity(
                request.getCity(),
                request.getState(),
                request.getMinCapacity()
            );
        }
        // Search by city and state
        else if (request.getCity() != null && request.getState() != null) {
            venues = venueService.findVenuesByCityAndState(request.getCity(), request.getState());
        }
        // Search by city only
        else if (request.getCity() != null) {
            venues = venueService.findVenuesByCity(request.getCity());
        }
        // Search by name
        else if (request.getName() != null) {
            venues = venueService.searchVenuesByName(request.getName());
        }
        else {
            return ResponseEntity.badRequest().build();
        }

        // Apply additional filters
        if (request.getVenueType() != null) {
            String type = request.getVenueType();
            venues = venues.stream()
                .filter(v -> v.getVenueType() != null && v.getVenueType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(request.getParkingRequired())) {
            venues = venues.stream()
                .filter(v -> Boolean.TRUE.equals(v.getParkingAvailable()))
                .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(request.getTransitRequired())) {
            venues = venues.stream()
                .filter(v -> Boolean.TRUE.equals(v.getPublicTransitNearby()))
                .collect(Collectors.toList());
        }

        List<VenueResponse> responses = venues.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-city")
    @RequiresPermission(RbacPermissions.VENUE_READ)
    @Operation(summary = "Find venues by city", description = "Find venues in a specific city")
    public ResponseEntity<List<VenueResponse>> getVenuesByCity(@RequestParam String city) {
        List<Venue> venues = venueService.findVenuesByCity(city);
        List<VenueResponse> responses = venues.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{venueId}")
    @RequiresPermission(value = RbacPermissions.VENUE_DELETE, resources = {"venue_id=#venueId"})
    @Operation(summary = "Delete venue", description = "Delete a venue (soft delete)")
    public ResponseEntity<Void> deleteVenue(
        @PathVariable UUID venueId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        venueService.deleteVenue(venueId);
        return ResponseEntity.noContent().build();
    }

    private Venue toEntity(VenueRequest request) {
        Venue venue = new Venue();
        venue.setName(request.getName());
        venue.setAddressLine1(request.getAddressLine1());
        venue.setAddressLine2(request.getAddressLine2());
        venue.setCity(request.getCity());
        venue.setState(request.getState());
        venue.setCountry(request.getCountry());
        venue.setPostalCode(request.getPostalCode());
        venue.setLatitude(request.getLatitude());
        venue.setLongitude(request.getLongitude());
        venue.setCapacity(request.getCapacity());
        venue.setVenueType(request.getVenueType());
        venue.setAccessibilityFeatures(request.getAccessibilityFeatures());
        venue.setDescription(request.getDescription());
        venue.setPhone(request.getPhone());
        venue.setEmail(request.getEmail());
        venue.setWebsiteUrl(request.getWebsiteUrl());
        venue.setParkingAvailable(request.getParkingAvailable());
        venue.setPublicTransitNearby(request.getPublicTransitNearby());
        return venue;
    }

    private VenueResponse toResponse(Venue entity) {
        VenueResponse response = new VenueResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setAddressLine1(entity.getAddressLine1());
        response.setAddressLine2(entity.getAddressLine2());
        response.setCity(entity.getCity());
        response.setState(entity.getState());
        response.setCountry(entity.getCountry());
        response.setPostalCode(entity.getPostalCode());
        response.setLatitude(entity.getLatitude());
        response.setLongitude(entity.getLongitude());
        response.setCapacity(entity.getCapacity());
        response.setVenueType(entity.getVenueType());
        response.setAccessibilityFeatures(entity.getAccessibilityFeatures());
        response.setDescription(entity.getDescription());
        response.setPhone(entity.getPhone());
        response.setEmail(entity.getEmail());
        response.setWebsiteUrl(entity.getWebsiteUrl());
        response.setParkingAvailable(entity.getParkingAvailable());
        response.setPublicTransitNearby(entity.getPublicTransitNearby());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
