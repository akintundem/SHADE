package eventplanner.security.auth.service;

import eventplanner.security.auth.dto.res.LocationSearchResponse;
import eventplanner.security.auth.entity.Location;
import eventplanner.security.auth.repository.LocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for searching available locations.
 */
@Service
public class LocationService {

    private static final int MAX_PAGE_SIZE = 50;
    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    /**
     * Searches for cities in North America matching the query.
     * Searches by city name, state/province name, or country.
     * 
     * @param query Search query (city, state, or country name)
     * @param pageable Pagination parameters
     * @return Paginated list of matching locations with coordinates and UUIDs
     */
    public Page<LocationSearchResponse> searchLocations(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            return Page.empty(pageable);
        }

        String normalizedQuery = query.trim();
        
        // Enforce max page size
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable adjustedPageable = pageable.getPageSize() > MAX_PAGE_SIZE 
                ? PageRequest.of(pageable.getPageNumber(), pageSize, pageable.getSort())
                : pageable;

        List<Location> allResults = locationRepository.searchByQuery(normalizedQuery);
        
        String lowerQuery = normalizedQuery.toLowerCase();
        List<LocationSearchResponse> sortedResults = allResults.stream()
                .map(this::toLocationSearchResponse)
                .sorted(Comparator
                        .comparing((LocationSearchResponse l) -> l.getCity().toLowerCase().startsWith(lowerQuery) ? 0 : 1)
                        .thenComparing(l -> l.getCity().toLowerCase()))
                .collect(Collectors.toList());

        // Calculate pagination
        int start = (int) adjustedPageable.getOffset();
        int end = Math.min((start + adjustedPageable.getPageSize()), sortedResults.size());
        
        List<LocationSearchResponse> pageContent = start < sortedResults.size() 
                ? sortedResults.subList(start, end)
                : new ArrayList<>();

        return new PageImpl<>(pageContent, adjustedPageable, sortedResults.size());
    }

    private LocationSearchResponse toLocationSearchResponse(Location location) {
        return LocationSearchResponse.builder()
                .id(location.getId())
                .city(location.getCity())
                .state(location.getState())
                .country(location.getCountry())
                .displayName(location.getDisplayName())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }
}

