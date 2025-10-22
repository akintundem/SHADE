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
            // Owner ID should come from authenticated user context, not random generation
            String ownerIdStr = (String) validatedData.get("ownerId");
            if (ownerIdStr != null && !ownerIdStr.trim().isEmpty()) {
                try {
                    event.setOwnerId(UUID.fromString(ownerIdStr));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid owner ID format: " + ownerIdStr);
                }
            } else {
                throw new IllegalArgumentException("Owner ID is required for event creation");
            } 
            
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
            
            // Set capacity with proper validation
            Object capacityObj = validatedData.get("capacity");
            if (capacityObj != null) {
                int capacity;
                if (capacityObj instanceof Integer) {
                    capacity = (Integer) capacityObj;
                } else if (capacityObj instanceof String) {
                    try {
                        capacity = Integer.parseInt((String) capacityObj);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid capacity format: " + capacityObj);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid capacity type: " + capacityObj.getClass().getSimpleName());
                }
                
                // Validate capacity range
                if (capacity < 0) {
                    throw new IllegalArgumentException("Capacity cannot be negative");
                }
                if (capacity > 100000) {
                    throw new IllegalArgumentException("Capacity too large (max 100,000)");
                }
                
                event.setCapacity(capacity);
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
     * Update an existing event
     * @param id The event ID
     * @param request The update request
     * @return Updated event
     */
    public Event update(UUID id, UpdateEventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + id));
        
        // Update only the fields that are provided (not null)
        if (request.getName() != null) {
            event.setName(request.getName());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventType() != null) {
            event.setEventType(request.getEventType());
        }
        if (request.getEventStatus() != null) {
            event.setEventStatus(request.getEventStatus());
        }
        if (request.getStartDateTime() != null) {
            event.setStartDateTime(request.getStartDateTime());
        }
        if (request.getEndDateTime() != null) {
            event.setEndDateTime(request.getEndDateTime());
        }
        if (request.getRegistrationDeadline() != null) {
            event.setRegistrationDeadline(request.getRegistrationDeadline());
        }
        if (request.getCapacity() != null) {
            event.setCapacity(request.getCapacity());
        }
        if (request.getCurrentAttendeeCount() != null) {
            event.setCurrentAttendeeCount(request.getCurrentAttendeeCount());
        }
        if (request.getIsPublic() != null) {
            event.setIsPublic(request.getIsPublic());
        }
        if (request.getRequiresApproval() != null) {
            event.setRequiresApproval(request.getRequiresApproval());
        }
        if (request.getQrCodeEnabled() != null) {
            event.setQrCodeEnabled(request.getQrCodeEnabled());
        }
        if (request.getQrCode() != null) {
            event.setQrCode(request.getQrCode());
        }
        if (request.getCoverImageUrl() != null) {
            event.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getEventWebsiteUrl() != null) {
            event.setEventWebsiteUrl(request.getEventWebsiteUrl());
        }
        if (request.getHashtag() != null) {
            event.setHashtag(request.getHashtag());
        }
        if (request.getTheme() != null) {
            event.setTheme(request.getTheme());
        }
        if (request.getObjectives() != null) {
            event.setObjectives(request.getObjectives());
        }
        if (request.getTargetAudience() != null) {
            event.setTargetAudience(request.getTargetAudience());
        }
        if (request.getSuccessMetrics() != null) {
            event.setSuccessMetrics(request.getSuccessMetrics());
        }
        if (request.getBrandingGuidelines() != null) {
            event.setBrandingGuidelines(request.getBrandingGuidelines());
        }
        if (request.getVenueRequirements() != null) {
            event.setVenueRequirements(request.getVenueRequirements());
        }
        if (request.getTechnicalRequirements() != null) {
            event.setTechnicalRequirements(request.getTechnicalRequirements());
        }
        if (request.getAccessibilityFeatures() != null) {
            event.setAccessibilityFeatures(request.getAccessibilityFeatures());
        }
        if (request.getEmergencyPlan() != null) {
            event.setEmergencyPlan(request.getEmergencyPlan());
        }
        if (request.getBackupPlan() != null) {
            event.setBackupPlan(request.getBackupPlan());
        }
        if (request.getPostEventTasks() != null) {
            event.setPostEventTasks(request.getPostEventTasks());
        }
        if (request.getMetadata() != null) {
            event.setMetadata(request.getMetadata());
        }
        if (request.getVenueId() != null) {
            event.setVenueId(request.getVenueId());
        }
        
        return eventRepository.save(event);
    }

    /**
     * Create event from CreateEventRequest
     * @param request The create event request
     * @param ownerId The owner ID
     * @return Created event
     */
    public Event create(CreateEventRequest request, UUID ownerId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Owner ID is required for event creation");
        }
        
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
     * Parse date time string to LocalDateTime with validation
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Date time cannot be empty");
        }
        
        try {
            // Try ISO format first
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Validate date is not in the past (for start date)
            if (dateTime.isBefore(LocalDateTime.now().minusHours(1))) {
                throw new IllegalArgumentException("Event date cannot be more than 1 hour in the past");
            }
            
            // Validate date is not too far in the future (max 10 years)
            if (dateTime.isAfter(LocalDateTime.now().plusYears(10))) {
                throw new IllegalArgumentException("Event date cannot be more than 10 years in the future");
            }
            
            return dateTime;
        } catch (Exception e) {
            try {
                // Try with 'T' separator
                LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
                
                // Apply same validations
                if (dateTime.isBefore(LocalDateTime.now().minusHours(1))) {
                    throw new IllegalArgumentException("Event date cannot be more than 1 hour in the past");
                }
                if (dateTime.isAfter(LocalDateTime.now().plusYears(10))) {
                    throw new IllegalArgumentException("Event date cannot be more than 10 years in the future");
                }
                
                return dateTime;
            } catch (Exception e2) {
                throw new IllegalArgumentException("Invalid date format: " + dateTimeStr + ". Expected ISO format (YYYY-MM-DDTHH:mm:ss)");
            }
        }
    }
}
