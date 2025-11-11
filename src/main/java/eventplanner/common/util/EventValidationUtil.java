package eventplanner.common.util;

import eventplanner.features.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utility class for event-related validations.
 * Provides reusable validation methods for event operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventValidationUtil {
    
    private final EventRepository eventRepository;
    
    /**
     * Validates that an event exists in the database.
     * 
     * @param eventId The event ID to validate
     * @throws IllegalArgumentException if eventId is null
     * @throws RuntimeException if event does not exist
     */
    public void validateEventExists(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (!eventRepository.existsById(eventId)) {
            log.warn("Event validation failed: Event not found with ID {}", eventId);
            throw new RuntimeException("Event not found: " + eventId);
        }
    }
    
    /**
     * Validates that an event exists and returns a boolean.
     * Does not throw exceptions, useful for conditional checks.
     * 
     * @param eventId The event ID to validate
     * @return true if event exists, false otherwise
     */
    public boolean eventExists(UUID eventId) {
        if (eventId == null) {
            return false;
        }
        return eventRepository.existsById(eventId);
    }
    
    /**
     * Validates that an event ID is not null.
     * 
     * @param eventId The event ID to validate
     * @param message Custom error message (optional)
     * @throws IllegalArgumentException if eventId is null
     */
    public void validateEventIdNotNull(UUID eventId, String message) {
        if (eventId == null) {
            String errorMessage = message != null ? message : "Event ID cannot be null";
            throw new IllegalArgumentException(errorMessage);
        }
    }
    
    /**
     * Validates that an event ID is not null.
     * 
     * @param eventId The event ID to validate
     * @throws IllegalArgumentException if eventId is null
     */
    public void validateEventIdNotNull(UUID eventId) {
        validateEventIdNotNull(eventId, null);
    }
}

