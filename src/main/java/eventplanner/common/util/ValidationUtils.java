package eventplanner.common.util;

import eventplanner.common.exception.exceptions.ForbiddenException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.common.exception.exceptions.UnauthorizedException;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.service.AuthorizationService;

import java.util.UUID;

/**
 * Common validation utilities to reduce code duplication across services.
 * Provides centralized validation logic for authentication, resource access, and common parameter checks.
 */
public class ValidationUtils {

    /**
     * Validate that user principal is authenticated.
     *
     * @param principal User principal
     * @throws UnauthorizedException if principal is null or has no ID
     */
    public static void requireAuthentication(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new UnauthorizedException("Authentication required");
        }
    }

    /**
     * Fetch event by ID or throw ResourceNotFoundException.
     *
     * @param eventId Event ID
     * @param eventRepository Event repository
     * @return Event entity
     * @throws ResourceNotFoundException if event not found
     */
    public static Event requireEvent(UUID eventId, EventRepository eventRepository) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }

    /**
     * Validate that user has access to event.
     *
     * @param eventId Event ID
     * @param principal User principal
     * @param authorizationService Authorization service
     * @throws ForbiddenException if user cannot access event
     */
    public static void requireEventAccess(UUID eventId, UserPrincipal principal,
                                         AuthorizationService authorizationService) {
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ForbiddenException("Access denied to event: " + eventId);
        }
    }

    /**
     * Validate that user can manage event (owner, organizer, or admin).
     *
     * @param eventId Event ID
     * @param principal User principal
     * @param authorizationService Authorization service
     * @throws ForbiddenException if user cannot manage event
     */
    public static void requireEventManagement(UUID eventId, UserPrincipal principal,
                                             AuthorizationService authorizationService) {
        if (!authorizationService.isEventOwner(principal, eventId) &&
            !authorizationService.hasEventMembership(principal, eventId) &&
            !authorizationService.isAdmin(principal)) {
            throw new ForbiddenException("Only event organizers can perform this action");
        }
    }

    /**
     * Validate that user is event owner or admin.
     *
     * @param eventId Event ID
     * @param principal User principal
     * @param authorizationService Authorization service
     * @throws ForbiddenException if user is not owner or admin
     */
    public static void requireEventOwnership(UUID eventId, UserPrincipal principal,
                                            AuthorizationService authorizationService) {
        if (!authorizationService.isEventOwner(principal, eventId) &&
            !authorizationService.isAdmin(principal)) {
            throw new ForbiddenException("Only event owners or admins can perform this action");
        }
    }

    /**
     * Validate non-null parameter.
     *
     * @param value Value to check
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if value is null
     */
    public static <T> void requireNonNull(T value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException(paramName + " is required");
        }
    }

    /**
     * Validate positive number.
     *
     * @param value Value to check
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if value is not positive
     */
    public static void requirePositive(Integer value, String paramName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(paramName + " must be positive");
        }
    }

    /**
     * Validate positive number (long).
     *
     * @param value Value to check
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if value is not positive
     */
    public static void requirePositive(Long value, String paramName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(paramName + " must be positive");
        }
    }

    /**
     * Validate non-negative number.
     *
     * @param value Value to check
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if value is negative
     */
    public static void requireNonNegative(Integer value, String paramName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(paramName + " must be non-negative");
        }
    }

    /**
     * Validate non-negative number (long).
     *
     * @param value Value to check
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if value is negative
     */
    public static void requireNonNegative(Long value, String paramName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(paramName + " must be non-negative");
        }
    }

    /**
     * Validate that string is not null or blank.
     *
     * @param value Value to check
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if value is null or blank
     */
    public static void requireNonBlank(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be blank");
        }
    }

    /**
     * Validate that value is within range (inclusive).
     *
     * @param value Value to check
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if value is out of range
     */
    public static void requireInRange(Integer value, int min, int max, String paramName) {
        if (value == null || value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("%s must be between %d and %d", paramName, min, max));
        }
    }

    /**
     * Validate that value is within range (inclusive) for long values.
     *
     * @param value Value to check
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if value is out of range
     */
    public static void requireInRange(Long value, long min, long max, String paramName) {
        if (value == null || value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("%s must be between %d and %d", paramName, min, max));
        }
    }
}
