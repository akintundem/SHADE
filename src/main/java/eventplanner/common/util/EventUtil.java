package eventplanner.common.util;

import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for common Event operations.
 * Provides helper methods to avoid repetitive code for fetching Event entities.
 */
public final class EventUtil {

    private EventUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Get Event from UUID.
     * 
     * @param eventId The event ID (can be null)
     * @param eventRepository The repository to fetch from
     * @return Optional containing the Event, or empty if eventId is null or event not found
     */
    public static Optional<Event> getEventById(UUID eventId, EventRepository eventRepository) {
        if (eventId == null) {
            return Optional.empty();
        }
        return eventRepository.findById(eventId);
    }

    /**
     * Get Event from UUID, throwing exception if not found.
     * 
     * @param eventId The event ID (can be null)
     * @param eventRepository The repository to fetch from
     * @param errorMessage Error message if event not found
     * @return The Event entity
     * @throws IllegalArgumentException if eventId is null or event not found
     */
    public static Event getEventByIdOrThrow(UUID eventId, EventRepository eventRepository, String errorMessage) {
        return getEventById(eventId, eventRepository)
            .orElseThrow(() -> new IllegalArgumentException(errorMessage != null ? errorMessage : "Event not found: " + eventId));
    }

    /**
     * Get Event from UUID, throwing exception if not found.
     * Uses default error message.
     * 
     * @param eventId The event ID (can be null)
     * @param eventRepository The repository to fetch from
     * @return The Event entity
     * @throws IllegalArgumentException if eventId is null or event not found
     */
    public static Event getEventByIdOrThrow(UUID eventId, EventRepository eventRepository) {
        return getEventByIdOrThrow(eventId, eventRepository, null);
    }

    /**
     * Get Event from UUID, throwing ResponseStatusException if not found.
     * Convenience method for controllers that use ResponseStatusException.
     * 
     * @param eventId The event ID (can be null)
     * @param eventRepository The repository to fetch from
     * @param errorMessage Error message if event not found
     * @return The Event entity
     * @throws ResponseStatusException if eventId is null or event not found
     */
    public static Event getEventByIdOrThrowResponseStatus(UUID eventId, EventRepository eventRepository, String errorMessage) {
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event ID is required");
        }
        return getEventById(eventId, eventRepository)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                errorMessage != null ? errorMessage : "Event not found: " + eventId));
    }

    /**
     * Get Event from UUID, throwing ResponseStatusException if not found.
     * Uses default error message.
     * 
     * @param eventId The event ID (can be null)
     * @param eventRepository The repository to fetch from
     * @return The Event entity
     * @throws ResponseStatusException if eventId is null or event not found
     */
    public static Event getEventByIdOrThrowResponseStatus(UUID eventId, EventRepository eventRepository) {
        return getEventByIdOrThrowResponseStatus(eventId, eventRepository, null);
    }
}

