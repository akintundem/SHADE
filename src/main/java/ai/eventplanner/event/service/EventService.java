package ai.eventplanner.event.service;

import ai.eventplanner.event.dto.request.CreateEventRequest;
import ai.eventplanner.event.dto.request.UpdateEventRequest;
import ai.eventplanner.event.dto.response.EventResponse;
import ai.eventplanner.event.entity.Event;
import ai.eventplanner.event.repo.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    /**
     * Create a new event from validated data
     * @param validatedData The validated event data from Python AI service
     * @param chatId The chat ID for tracking
     * @return EventResponse with created event details
     */
    public EventResponse createEvent(Map<String, Object> validatedData, String chatId) {
        try {
            // Create new event entity
            Event event = new Event();
            
            // Set basic fields
            event.setName((String) validatedData.get("name"));
            event.setDescription((String) validatedData.get("description"));
            event.setOwnerId(UUID.randomUUID()); // TODO: Get from authenticated user
            
            // Parse event type
            String eventTypeStr = (String) validatedData.get("eventType");
            if (eventTypeStr != null) {
                try {
                    event.setEventType(ai.eventplanner.common.domain.enums.EventType.valueOf(eventTypeStr));
                } catch (IllegalArgumentException e) {
                    event.setEventType(ai.eventplanner.common.domain.enums.EventType.MEETING);
                }
            }
            
            // Parse dates
            String startDateTimeStr = (String) validatedData.get("startDateTime");
            if (startDateTimeStr != null) {
                event.setStartDateTime(parseDateTime(startDateTimeStr));
            }
            
            String endDateTimeStr = (String) validatedData.get("endDateTime");
            if (endDateTimeStr != null) {
                event.setEndDateTime(parseDateTime(endDateTimeStr));
            }
            
            // Set capacity
            Object capacityObj = validatedData.get("capacity");
            if (capacityObj != null) {
                if (capacityObj instanceof Integer) {
                    event.setCapacity((Integer) capacityObj);
                } else if (capacityObj instanceof String) {
                    try {
                        event.setCapacity(Integer.parseInt((String) capacityObj));
                    } catch (NumberFormatException e) {
                        // Ignore invalid capacity
                    }
                }
            }
            
            // Set boolean fields
            event.setIsPublic((Boolean) validatedData.getOrDefault("isPublic", true));
            event.setRequiresApproval((Boolean) validatedData.getOrDefault("requiresApproval", false));
            event.setQrCodeEnabled((Boolean) validatedData.getOrDefault("qrCodeEnabled", false));
            
            // Handle venue data if provided
            if (validatedData.containsKey("venues") && validatedData.get("venues") != null) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> venues = (java.util.List<Map<String, Object>>) validatedData.get("venues");
                if (!venues.isEmpty()) {
                    // Store the first venue as the selected venue
                    Map<String, Object> selectedVenue = venues.get(0);
                    // Note: The Event entity doesn't have venue fields, so we'll store in description for now
                    String venueInfo = String.format("Venue: %s, Address: %s", 
                        selectedVenue.get("name"), selectedVenue.get("address"));
                    if (event.getDescription() != null) {
                        event.setDescription(event.getDescription() + "\n\n" + venueInfo);
                    } else {
                        event.setDescription(venueInfo);
                    }
                }
            }
            
            // Save to database
            Event savedEvent = eventRepository.save(event);
            
            // Convert to response
            return toResponse(savedEvent);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create event: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing event
     * @param eventId The ID of the event to update
     * @param validatedData The validated update data
     * @return EventResponse with updated event details
     */
    public EventResponse updateEvent(String eventId, Map<String, Object> validatedData) {
        try {
            UUID uuid = UUID.fromString(eventId);
            Optional<Event> eventOpt = eventRepository.findById(uuid);
            
            if (eventOpt.isEmpty()) {
                throw new RuntimeException("Event not found with ID: " + eventId);
            }
            
            Event event = eventOpt.get();
            
            // Update fields if provided
            if (validatedData.containsKey("name")) {
                event.setName((String) validatedData.get("name"));
            }
            if (validatedData.containsKey("description")) {
                event.setDescription((String) validatedData.get("description"));
            }
            if (validatedData.containsKey("capacity")) {
                Object capacityObj = validatedData.get("capacity");
                if (capacityObj instanceof Integer) {
                    event.setCapacity((Integer) capacityObj);
                } else if (capacityObj instanceof String) {
                    try {
                        event.setCapacity(Integer.parseInt((String) capacityObj));
                    } catch (NumberFormatException e) {
                        // Ignore invalid capacity
                    }
                }
            }
            
            // Save updated event
            Event savedEvent = eventRepository.save(event);
            
            // Convert to response
            return toResponse(savedEvent);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to update event: " + e.getMessage(), e);
        }
    }

    /**
     * Get event by chat ID
     * @param chatId The chat ID
     * @return EventResponse or null if not found
     */
    public EventResponse getEventByChatId(String chatId) {
        // For now, return null as we don't have chat ID tracking in the Event entity
        // This would need to be implemented based on your requirements
        return null;
    }

    /**
     * Get event by ID
     * @param id The event ID
     * @return Optional containing the event if found
     */
    public Optional<Event> getById(UUID id) {
        return eventRepository.findById(id);
    }

    /**
     * Create event from CreateEventRequest
     * @param request The create event request
     * @param ownerId The owner ID
     * @return Created event
     */
    public Event create(CreateEventRequest request, UUID ownerId) {
        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setOwnerId(ownerId);
        event.setEventType(request.getEventType());
        event.setEventStatus(request.getEventStatus());
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setCapacity(request.getCapacity());
        event.setIsPublic(request.getIsPublic());
        event.setRequiresApproval(request.getRequiresApproval());
        event.setQrCodeEnabled(request.getQrCodeEnabled());
        
        return eventRepository.save(event);
    }

    /**
     * Delete event by ID
     * @param id The event ID
     */
    public void delete(UUID id) {
        eventRepository.deleteById(id);
    }

    /**
     * Convert Event entity to EventResponse
     */
    public EventResponse toResponse(Event event) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setDescription(event.getDescription());
        response.setEventType(event.getEventType());
        response.setEventStatus(event.getEventStatus());
        response.setStartDateTime(event.getStartDateTime());
        response.setEndDateTime(event.getEndDateTime());
        response.setCapacity(event.getCapacity());
        response.setCurrentAttendeeCount(event.getCurrentAttendeeCount());
        response.setIsPublic(event.getIsPublic());
        response.setRequiresApproval(event.getRequiresApproval());
        response.setQrCodeEnabled(event.getQrCodeEnabled());
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        return response;
    }

    /**
     * Parse date time string to LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // Try ISO format first
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            try {
                // Try with 'T' separator
                return LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
            } catch (Exception e2) {
                throw new RuntimeException("Invalid date format: " + dateTimeStr);
            }
        }
    }
}
