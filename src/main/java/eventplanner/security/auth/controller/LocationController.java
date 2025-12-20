package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.res.LocationSearchResponse;
import eventplanner.security.auth.service.LocationService;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for location search functionality.
 * Provides endpoints for searching cities in North America.
 */
@RestController
@RequestMapping("/api/v1/auth/locations")
@Validated
@Tag(name = "Location Search", description = "Endpoints for searching cities in North America")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * Search for cities in North America.
     * Supports searching by city name, state/province, or country.
     * Results are sorted with exact city name matches first.
     * 
     * @param query Search query (city, state, or country name)
     * @param pageable Pagination parameters (page, size, sort)
     * @return Paginated list of matching locations
     */
    @GetMapping("/search")
    @RequiresPermission(RbacPermissions.USER_SEARCH)
    @Operation(
        summary = "Search locations",
        description = "Search for cities in North America (United States, Canada, Mexico). " +
                     "Supports searching by city name, state/province name, or country. " +
                     "Results are sorted with exact city name matches appearing first. " +
                     "Useful for dropdown/autocomplete functionality when users select their location. " +
                     "Supports pagination via page and size query parameters."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<Page<LocationSearchResponse>> searchLocations(
            @Parameter(description = "Search query (city, state, or country name)", example = "New York")
            @RequestParam(required = false)
            String query,
            @Parameter(description = "Pagination parameters (page, size, sort). Default size: 20, max size: 50")
            @PageableDefault(size = 20) Pageable pageable) {
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(Page.empty(pageable));
        }

        // Validate query length if provided
        String trimmedQuery = query.trim();
        if (trimmedQuery.length() > 100) {
            return ResponseEntity.badRequest().build();
        }

        Page<LocationSearchResponse> results = locationService.searchLocations(trimmedQuery, pageable);
        return ResponseEntity.ok(results);
    }
}

