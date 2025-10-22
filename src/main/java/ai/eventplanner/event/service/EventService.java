package ai.eventplanner.event.service;

import ai.eventplanner.event.dto.request.CreateEventRequest;
import ai.eventplanner.event.dto.request.UpdateEventRequest;
import ai.eventplanner.event.dto.request.EventCreationRequest;
import ai.eventplanner.event.dto.request.EventUpdateRequest;
import ai.eventplanner.event.dto.response.EventResponse;
import ai.eventplanner.event.entity.Event;
import ai.eventplanner.event.repo.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

@Service
@Transactional
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    /**
     * Create a new event from structured DTO
     * @param request The validated event creation request
     * @return EventResponse with created event details
     */
    public EventResponse createEvent(EventCreationRequest request) {
        try {
            // Create new event entity
            Event event = new Event();
            
            // Set basic fields
            event.setName(request.getName());
            event.setDescription(request.getDescription());
            
            // Validate and set owner ID
            UUID ownerId = UUID.fromString(request.getOwnerId());
            event.setOwnerId(ownerId);
            
            // Set event type
            if (request.getEventType() != null) {
                try {
                    event.setEventType(ai.eventplanner.common.domain.enums.EventType.valueOf(request.getEventType()));
                } catch (IllegalArgumentException e) {
                    event.setEventType(ai.eventplanner.common.domain.enums.EventType.MEETING);
                }
            }
            
            // Set dates
            event.setStartDateTime(request.getStartDateTime());
            if (request.getEndDateTime() != null) {
                event.setEndDateTime(request.getEndDateTime());
            }
            
            // Set capacity
            if (request.getCapacity() != null) {
                event.setCapacity(request.getCapacity());
            }
            
            // Set boolean fields
            event.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);
            event.setRequiresApproval(request.getRequiresApproval() != null ? request.getRequiresApproval() : false);
            event.setQrCodeEnabled(request.getQrCodeEnabled() != null ? request.getQrCodeEnabled() : false);
            
            // Handle venue data if provided
            if (request.getVenues() != null && !request.getVenues().isEmpty()) {
                EventCreationRequest.VenueRequest selectedVenue = request.getVenues().get(0);
                String venueInfo = String.format("Venue: %s, Address: %s", 
                    selectedVenue.getName(), selectedVenue.getAddress());
                if (event.getDescription() != null) {
                    event.setDescription(event.getDescription() + "\n\n" + venueInfo);
                } else {
                    event.setDescription(venueInfo);
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
     * @param request The validated update request
     * @return EventResponse with updated event details
     */
    public EventResponse updateEvent(String eventId, EventUpdateRequest request) {
        try {
            UUID uuid = UUID.fromString(eventId);
            Optional<Event> eventOpt = eventRepository.findById(uuid);
            
            if (eventOpt.isEmpty()) {
                throw new RuntimeException("Event not found with ID: " + eventId);
            }
            
            Event event = eventOpt.get();
            
            // Update fields if provided
            if (request.getName() != null) {
                event.setName(request.getName());
            }
            if (request.getDescription() != null) {
                event.setDescription(request.getDescription());
            }
            if (request.getCapacity() != null) {
                event.setCapacity(request.getCapacity());
            }
            if (request.getStartDateTime() != null) {
                event.setStartDateTime(request.getStartDateTime());
            }
            if (request.getEndDateTime() != null) {
                event.setEndDateTime(request.getEndDateTime());
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
     * Convert Map data to EventCreationRequest DTO
     * This method is used for backward compatibility with existing Map-based data
     */
    public EventCreationRequest convertMapToEventCreationRequest(Map<String, Object> validatedData) {
        EventCreationRequest request = new EventCreationRequest();
        
        request.setName((String) validatedData.get("name"));
        request.setDescription((String) validatedData.get("description"));
        request.setOwnerId((String) validatedData.get("ownerId"));
        request.setEventType((String) validatedData.get("eventType"));
        
        // Parse dates
        String startDateTimeStr = (String) validatedData.get("startDateTime");
        if (startDateTimeStr != null) {
            request.setStartDateTime(LocalDateTime.parse(startDateTimeStr));
        }
        
        String endDateTimeStr = (String) validatedData.get("endDateTime");
        if (endDateTimeStr != null) {
            request.setEndDateTime(LocalDateTime.parse(endDateTimeStr));
        }
        
        // Parse capacity
        Object capacityObj = validatedData.get("capacity");
        if (capacityObj instanceof Integer) {
            request.setCapacity((Integer) capacityObj);
        } else if (capacityObj instanceof String) {
            try {
                request.setCapacity(Integer.parseInt((String) capacityObj));
            } catch (NumberFormatException e) {
                // Ignore invalid capacity
            }
        }
        
        // Set boolean fields
        request.setIsPublic((Boolean) validatedData.getOrDefault("isPublic", true));
        request.setRequiresApproval((Boolean) validatedData.getOrDefault("requiresApproval", false));
        request.setQrCodeEnabled((Boolean) validatedData.getOrDefault("qrCodeEnabled", false));
        
        // Handle venues
        if (validatedData.containsKey("venues") && validatedData.get("venues") != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> venues = (List<Map<String, Object>>) validatedData.get("venues");
            List<EventCreationRequest.VenueRequest> venueRequests = new ArrayList<>();
            
            for (Map<String, Object> venue : venues) {
                EventCreationRequest.VenueRequest venueRequest = new EventCreationRequest.VenueRequest();
                venueRequest.setName((String) venue.get("name"));
                venueRequest.setAddress((String) venue.get("address"));
                venueRequest.setDescription((String) venue.get("description"));
                venueRequest.setContactEmail((String) venue.get("contactEmail"));
                venueRequest.setContactPhone((String) venue.get("contactPhone"));
                venueRequest.setWebsite((String) venue.get("website"));
                venueRequests.add(venueRequest);
            }
            request.setVenues(venueRequests);
        }
        
        return request;
    }
    
    /**
     * Convert Map data to EventUpdateRequest DTO
     * This method is used for backward compatibility with existing Map-based data
     */
    public EventUpdateRequest convertMapToEventUpdateRequest(Map<String, Object> validatedData) {
        EventUpdateRequest request = new EventUpdateRequest();
        
        if (validatedData.containsKey("name")) {
            request.setName((String) validatedData.get("name"));
        }
        if (validatedData.containsKey("description")) {
            request.setDescription((String) validatedData.get("description"));
        }
        if (validatedData.containsKey("eventType")) {
            request.setEventType((String) validatedData.get("eventType"));
        }
        
        // Parse dates
        String startDateTimeStr = (String) validatedData.get("startDateTime");
        if (startDateTimeStr != null) {
            request.setStartDateTime(LocalDateTime.parse(startDateTimeStr));
        }
        
        String endDateTimeStr = (String) validatedData.get("endDateTime");
        if (endDateTimeStr != null) {
            request.setEndDateTime(LocalDateTime.parse(endDateTimeStr));
        }
        
        // Parse capacity
        Object capacityObj = validatedData.get("capacity");
        if (capacityObj instanceof Integer) {
            request.setCapacity((Integer) capacityObj);
        } else if (capacityObj instanceof String) {
            try {
                request.setCapacity(Integer.parseInt((String) capacityObj));
            } catch (NumberFormatException e) {
                // Ignore invalid capacity
            }
        }
        
        // Set boolean fields
        if (validatedData.containsKey("isPublic")) {
            request.setIsPublic((Boolean) validatedData.get("isPublic"));
        }
        if (validatedData.containsKey("requiresApproval")) {
            request.setRequiresApproval((Boolean) validatedData.get("requiresApproval"));
        }
        if (validatedData.containsKey("qrCodeEnabled")) {
            request.setQrCodeEnabled((Boolean) validatedData.get("qrCodeEnabled"));
        }
        
        return request;
    }

}

