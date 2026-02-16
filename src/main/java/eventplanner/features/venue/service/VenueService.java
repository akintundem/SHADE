package eventplanner.features.venue.service;

import eventplanner.common.util.GeoUtils;
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
import java.util.UUID;

/**
 * Service for managing venues.
 * Handles venue CRUD operations, search, and PostGIS-powered spatial queries.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class VenueService {

    private static final double DEFAULT_RADIUS_KM = 10.0;

    private final VenueRepository venueRepository;

    /**
     * Create a new venue
     */
    @Transactional(readOnly = false)
    public Venue createVenue(Venue venue) {
        validateVenue(venue);

        log.info("Creating new venue: {}", venue.getName());
        return venueRepository.save(venue);
    }

    /**
     * Update an existing venue
     */
    @Transactional(readOnly = false)
    public Venue updateVenue(UUID venueId, Venue updatedVenue) {
        log.info("Updating venue: {}", venueId);

        Venue venue = getVenueById(venueId);

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
     * PostGIS: find venues within a geographic bounding box.
     * Delegates to PostGIS ST_Within + ST_MakeEnvelope.
     */
    public List<Venue> findVenuesWithinBounds(
        BigDecimal minLatitude,
        BigDecimal maxLatitude,
        BigDecimal minLongitude,
        BigDecimal maxLongitude,
        String venueType,
        boolean parkingRequired,
        boolean transitRequired
    ) {
        log.debug("PostGIS: finding venues within bounds: lat [{}, {}], lng [{}, {}]",
            minLatitude, maxLatitude, minLongitude, maxLongitude);

        validateGeoCoordinates(minLatitude, maxLatitude, minLongitude, maxLongitude);

        return venueRepository.findWithinBounds(
            minLatitude.doubleValue(),
            maxLatitude.doubleValue(),
            minLongitude.doubleValue(),
            maxLongitude.doubleValue(),
            venueType,
            parkingRequired,
            transitRequired
        );
    }

    /**
     * PostGIS: find venues near a specific location within a radius in kilometres.
     * Uses ST_DWithin with geography cast for accurate great-circle distance.
     */
    public List<Venue> findVenuesNearLocation(
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal radiusKm,
        String venueType,
        boolean parkingRequired,
        boolean transitRequired
    ) {
        validateSinglePointCoordinates(latitude, longitude);

        double radiusMeters = radiusKm != null
            ? GeoUtils.kmToMeters(radiusKm)
            : GeoUtils.kmToMeters(DEFAULT_RADIUS_KM);

        log.debug("PostGIS: finding venues near ({}, {}) within {} m",
            latitude, longitude, radiusMeters);

        return venueRepository.findNearLocation(
            latitude.doubleValue(),
            longitude.doubleValue(),
            radiusMeters,
            venueType,
            parkingRequired,
            transitRequired
        );
    }

    /**
     * PostGIS: calculate distance in metres between a venue and a point.
     */
    public Double calculateDistanceMeters(UUID venueId, BigDecimal latitude, BigDecimal longitude) {
        return venueRepository.calculateDistanceMeters(
            venueId,
            latitude.doubleValue(),
            longitude.doubleValue()
        );
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
    @Transactional(readOnly = false)
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

        if (venue.getLatitude() != null || venue.getLongitude() != null) {
            if (venue.getLatitude() == null || venue.getLongitude() == null) {
                throw new BadRequestException("Both latitude and longitude must be provided");
            }

            validateSinglePointCoordinates(venue.getLatitude(), venue.getLongitude());
        }

        if (venue.getCapacity() != null && venue.getCapacity() < 0) {
            throw new BadRequestException("Capacity cannot be negative");
        }
    }

    /**
     * Validate a single geographic coordinate point (latitude + longitude).
     */
    private void validateSinglePointCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 ||
            latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BadRequestException("Latitude must be between -90 and 90");
        }
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 ||
            longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("Longitude must be between -180 and 180");
        }
    }

    /**
     * Validate geographic coordinate ranges (bounding box).
     */
    private void validateGeoCoordinates(
        BigDecimal minLat,
        BigDecimal maxLat,
        BigDecimal minLng,
        BigDecimal maxLng
    ) {
        if (minLat.compareTo(BigDecimal.valueOf(-90)) < 0 ||
            maxLat.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BadRequestException("Latitude must be between -90 and 90");
        }

        if (minLng.compareTo(BigDecimal.valueOf(-180)) < 0 ||
            maxLng.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("Longitude must be between -180 and 180");
        }

        if (minLat.compareTo(maxLat) > 0) {
            throw new BadRequestException("Minimum latitude must be less than maximum latitude");
        }
        if (minLng.compareTo(maxLng) > 0) {
            throw new BadRequestException("Minimum longitude must be less than maximum longitude");
        }
    }
}
