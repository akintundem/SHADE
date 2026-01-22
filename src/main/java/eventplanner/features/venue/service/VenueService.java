package eventplanner.features.venue.service;

import eventplanner.features.venue.entity.Venue;
import eventplanner.features.venue.repository.VenueRepository;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing venues.
 * Handles venue CRUD operations, search, and geo-spatial queries.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class VenueService {

    private final VenueRepository venueRepository;

    /**
     * Create a new venue
     */
    public Venue createVenue(Venue venue) {
        validateVenue(venue);

        log.info("Creating new venue: {}", venue.getName());
        return venueRepository.save(venue);
    }

    /**
     * Update an existing venue
     */
    public Venue updateVenue(UUID venueId, Venue updatedVenue) {
        log.info("Updating venue: {}", venueId);

        Venue venue = getVenueById(venueId);

        // Update fields
        if (updatedVenue.getName() != null) {
            venue.setName(updatedVenue.getName());
        }
        if (updatedVenue.getAddressLine1() != null) {
            venue.setAddressLine1(updatedVenue.getAddressLine1());
        }
        if (updatedVenue.getAddressLine2() != null) {
            venue.setAddressLine2(updatedVenue.getAddressLine2());
        }
        if (updatedVenue.getCity() != null) {
            venue.setCity(updatedVenue.getCity());
        }
        if (updatedVenue.getState() != null) {
            venue.setState(updatedVenue.getState());
        }
        if (updatedVenue.getCountry() != null) {
            venue.setCountry(updatedVenue.getCountry());
        }
        if (updatedVenue.getPostalCode() != null) {
            venue.setPostalCode(updatedVenue.getPostalCode());
        }
        if (updatedVenue.getLatitude() != null) {
            venue.setLatitude(updatedVenue.getLatitude());
        }
        if (updatedVenue.getLongitude() != null) {
            venue.setLongitude(updatedVenue.getLongitude());
        }
        if (updatedVenue.getCapacity() != null) {
            venue.setCapacity(updatedVenue.getCapacity());
        }
        if (updatedVenue.getVenueType() != null) {
            venue.setVenueType(updatedVenue.getVenueType());
        }
        if (updatedVenue.getAccessibilityFeatures() != null) {
            venue.setAccessibilityFeatures(updatedVenue.getAccessibilityFeatures());
        }
        if (updatedVenue.getDescription() != null) {
            venue.setDescription(updatedVenue.getDescription());
        }
        if (updatedVenue.getPhone() != null) {
            venue.setPhone(updatedVenue.getPhone());
        }
        if (updatedVenue.getEmail() != null) {
            venue.setEmail(updatedVenue.getEmail());
        }
        if (updatedVenue.getWebsiteUrl() != null) {
            venue.setWebsiteUrl(updatedVenue.getWebsiteUrl());
        }
        if (updatedVenue.getParkingAvailable() != null) {
            venue.setParkingAvailable(updatedVenue.getParkingAvailable());
        }
        if (updatedVenue.getPublicTransitNearby() != null) {
            venue.setPublicTransitNearby(updatedVenue.getPublicTransitNearby());
        }

        return venueRepository.save(venue);
    }

    /**
     * Get venue by ID
     */
    public Venue getVenueById(UUID venueId) {
        return venueRepository.findById(venueId)
            .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + venueId));
    }

    /**
     * Get all venues (paginated)
     */
    public Page<Venue> getAllVenues(Pageable pageable) {
        log.debug("Fetching all venues with pagination");
        return venueRepository.findAll(pageable);
    }

    /**
     * Search venues by name
     */
    public List<Venue> searchVenuesByName(String name) {
        log.debug("Searching venues by name: {}", name);
        return venueRepository.searchByName(name);
    }

    /**
     * Find venues by city
     */
    public List<Venue> findVenuesByCity(String city) {
        log.debug("Finding venues in city: {}", city);
        return venueRepository.findByCity(city);
    }

    /**
     * Find venues by city and state
     */
    public List<Venue> findVenuesByCityAndState(String city, String state) {
        log.debug("Finding venues in city: {}, state: {}", city, state);
        return venueRepository.findByCityAndState(city, state);
    }

    /**
     * Find venues within geographic bounds (bounding box)
     * Useful for "find venues near me" functionality
     */
    public List<Venue> findVenuesWithinBounds(
        BigDecimal minLatitude,
        BigDecimal maxLatitude,
        BigDecimal minLongitude,
        BigDecimal maxLongitude
    ) {
        log.debug("Finding venues within bounds: lat [{}, {}], lng [{}, {}]",
            minLatitude, maxLatitude, minLongitude, maxLongitude);

        validateGeoCoordinates(minLatitude, maxLatitude, minLongitude, maxLongitude);

        return venueRepository.findWithinBounds(minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    /**
     * Find venues near a specific location (within radius)
     * Radius is approximate in degrees (~111km per degree latitude)
     */
    public List<Venue> findVenuesNearLocation(
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal radiusDegrees
    ) {
        log.debug("Finding venues near location: ({}, {}) within radius: {}",
            latitude, longitude, radiusDegrees);

        BigDecimal minLat = latitude.subtract(radiusDegrees);
        BigDecimal maxLat = latitude.add(radiusDegrees);
        BigDecimal minLng = longitude.subtract(radiusDegrees);
        BigDecimal maxLng = longitude.add(radiusDegrees);

        return findVenuesWithinBounds(minLat, maxLat, minLng, maxLng);
    }

    /**
     * Find venues with minimum capacity
     */
    public List<Venue> findVenuesWithMinCapacity(String city, String state, Integer minCapacity) {
        log.debug("Finding venues in {}, {} with capacity >= {}", city, state, minCapacity);

        List<Venue> venues = venueRepository.findByCityAndState(city, state);
        return venues.stream()
            .filter(v -> v.getCapacity() != null && v.getCapacity() >= minCapacity)
            .toList();
    }

    /**
     * Delete venue (soft delete)
     */
    public void deleteVenue(UUID venueId) {
        log.info("Deleting venue: {}", venueId);

        Venue venue = getVenueById(venueId);
        venueRepository.delete(venue);
    }

    /**
     * Check if venue exists
     */
    public boolean venueExists(UUID venueId) {
        return venueRepository.existsById(venueId);
    }

    /**
     * Validate venue data
     */
    private void validateVenue(Venue venue) {
        if (venue.getName() == null || venue.getName().isBlank()) {
            throw new BadRequestException("Venue name is required");
        }

        // Validate coordinates if provided
        if (venue.getLatitude() != null || venue.getLongitude() != null) {
            if (venue.getLatitude() == null || venue.getLongitude() == null) {
                throw new BadRequestException("Both latitude and longitude must be provided");
            }

            validateGeoCoordinates(
                venue.getLatitude(), venue.getLatitude(),
                venue.getLongitude(), venue.getLongitude()
            );
        }

        // Validate capacity if provided
        if (venue.getCapacity() != null && venue.getCapacity() < 0) {
            throw new BadRequestException("Capacity cannot be negative");
        }
    }

    /**
     * Validate geographic coordinates
     */
    private void validateGeoCoordinates(
        BigDecimal minLat,
        BigDecimal maxLat,
        BigDecimal minLng,
        BigDecimal maxLng
    ) {
        // Latitude must be between -90 and 90
        if (minLat.compareTo(BigDecimal.valueOf(-90)) < 0 ||
            maxLat.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BadRequestException("Latitude must be between -90 and 90");
        }

        // Longitude must be between -180 and 180
        if (minLng.compareTo(BigDecimal.valueOf(-180)) < 0 ||
            maxLng.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("Longitude must be between -180 and 180");
        }

        // Min must be less than max
        if (minLat.compareTo(maxLat) > 0) {
            throw new BadRequestException("Minimum latitude must be less than maximum latitude");
        }
        if (minLng.compareTo(maxLng) > 0) {
            throw new BadRequestException("Minimum longitude must be less than maximum longitude");
        }
    }
}
