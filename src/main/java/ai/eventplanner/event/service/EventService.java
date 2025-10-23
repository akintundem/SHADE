package ai.eventplanner.event.service;

import ai.eventplanner.event.dto.request.CreateEventRequest;
import ai.eventplanner.event.dto.request.UpdateEventRequest;
import ai.eventplanner.event.dto.request.EventCreationRequest;
import ai.eventplanner.event.dto.request.EventUpdateRequest;
import ai.eventplanner.event.dto.response.EventResponse;
import ai.eventplanner.event.dto.response.UserEventRelationshipResponse;
import ai.eventplanner.event.dto.response.UserEventsSummaryResponse;
import ai.eventplanner.event.entity.Event;
import ai.eventplanner.event.repo.EventRepository;
import ai.eventplanner.common.domain.enums.EventStatus;
import ai.eventplanner.common.domain.enums.EventUserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

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

    // ==================== USER-EVENT RELATIONSHIP METHODS ====================

    /**
     * Get all events for a specific user
     */
    public List<Event> getEventsByUser(UUID userId) {
        return eventRepository.findEventsByOwner(userId);
    }

    /**
     * Get events owned by a user
     */
    public List<Event> getEventsOwnedByUser(UUID userId) {
        return eventRepository.findEventsByOwner(userId);
    }

    /**
     * Get upcoming events for a user
     */
    public List<Event> getUpcomingEventsByUser(UUID userId) {
        return eventRepository.findUpcomingEventsByOwner(userId, LocalDateTime.now());
    }

    /**
     * Get past events for a user
     */
    public List<Event> getPastEventsByUser(UUID userId) {
        return eventRepository.findPastEventsByOwner(userId, LocalDateTime.now());
    }

    /**
     * Get user's events summary
     */
    public UserEventsSummaryResponse getUserEventsSummary(UUID userId) {
        UserEventsSummaryResponse summary = new UserEventsSummaryResponse();
        
        List<Event> ownedEvents = getEventsOwnedByUser(userId);
        List<Event> upcomingEvents = getUpcomingEventsByUser(userId);
        List<Event> pastEvents = getPastEventsByUser(userId);
        
        summary.setOwnedEvents(convertToUserEventRelationship(ownedEvents, EventUserType.ORGANIZER));
        summary.setUpcomingEvents(convertToUserEventRelationship(upcomingEvents, EventUserType.ORGANIZER));
        summary.setPastEvents(convertToUserEventRelationship(pastEvents, EventUserType.ORGANIZER));
        
        // For now, we'll set empty lists for other relationship types
        // These would be populated by joining with EventUser/EventAttendance tables
        summary.setAttendingEvents(new ArrayList<>());
        summary.setOrganizingEvents(new ArrayList<>());
        summary.setCoordinatingEvents(new ArrayList<>());
        summary.setVolunteeringEvents(new ArrayList<>());
        summary.setSpeakingEvents(new ArrayList<>());
        summary.setSponsoringEvents(new ArrayList<>());
        
        // Set counts
        summary.setTotalCount(ownedEvents.size());
        summary.setOwnedCount(ownedEvents.size());
        summary.setAttendingCount(0);
        summary.setOrganizingCount(0);
        summary.setCoordinatingCount(0);
        summary.setVolunteeringCount(0);
        summary.setSpeakingCount(0);
        summary.setSponsoringCount(0);
        
        return summary;
    }

    // ==================== EVENT STATUS & LIFECYCLE METHODS ====================

    /**
     * Update event status
     */
    public Event updateEventStatus(UUID eventId, EventStatus newStatus) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        event.setEventStatus(newStatus);
        return eventRepository.save(event);
    }

    /**
     * Publish event
     */
    public Event publishEvent(UUID eventId) {
        return updateEventStatus(eventId, EventStatus.PUBLISHED);
    }

    /**
     * Cancel event
     */
    public Event cancelEvent(UUID eventId) {
        return updateEventStatus(eventId, EventStatus.CANCELLED);
    }

    /**
     * Complete event
     */
    public Event completeEvent(UUID eventId) {
        return updateEventStatus(eventId, EventStatus.COMPLETED);
    }

    /**
     * Open registration
     */
    public Event openRegistration(UUID eventId) {
        return updateEventStatus(eventId, EventStatus.REGISTRATION_OPEN);
    }

    /**
     * Close registration
     */
    public Event closeRegistration(UUID eventId) {
        return updateEventStatus(eventId, EventStatus.REGISTRATION_CLOSED);
    }

    // ==================== EVENT DISCOVERY & SEARCH METHODS ====================

    /**
     * Search events
     */
    public List<Event> searchEvents(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return eventRepository.searchEvents(searchTerm.trim());
    }

    /**
     * Search public events
     */
    public List<Event> searchPublicEvents(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return eventRepository.findByIsPublicTrue();
        }
        return eventRepository.searchPublicEvents(searchTerm.trim());
    }

    /**
     * Get public events
     */
    public List<Event> getPublicEvents() {
        return eventRepository.findByIsPublicTrue();
    }

    /**
     * Get featured events
     */
    public List<Event> getFeaturedEvents(Pageable pageable) {
        return eventRepository.findFeaturedEvents(pageable);
    }

    /**
     * Get trending events
     */
    public List<Event> getTrendingEvents(Pageable pageable) {
        return eventRepository.findTrendingEvents(pageable);
    }

    /**
     * Get upcoming events
     */
    public List<Event> getUpcomingEvents() {
        return eventRepository.findByStartDateTimeAfterAndIsPublicTrue(LocalDateTime.now());
    }

    /**
     * Get events by type
     */
    public List<Event> getEventsByType(String eventType) {
        return eventRepository.findByEventType(eventType);
    }

    /**
     * Get events by status
     */
    public List<Event> getEventsByStatus(String eventStatus) {
        return eventRepository.findByEventStatus(eventStatus);
    }

    /**
     * Get events by date range
     */
    public List<Event> getEventsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return eventRepository.findByStartDateTimeBetween(startDate, endDate);
    }

    // ==================== EVENT CAPACITY & REGISTRATION METHODS ====================

    /**
     * Update event capacity
     */
    public Event updateCapacity(UUID eventId, Integer newCapacity) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        event.setCapacity(newCapacity);
        return eventRepository.save(event);
    }

    /**
     * Get available capacity
     */
    public Integer getAvailableCapacity(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        if (event.getCapacity() == null) {
            return null;
        }
        
        int currentCount = event.getCurrentAttendeeCount() != null ? event.getCurrentAttendeeCount() : 0;
        return Math.max(0, event.getCapacity() - currentCount);
    }

    /**
     * Update registration deadline
     */
    public Event updateRegistrationDeadline(UUID eventId, LocalDateTime deadline) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        event.setRegistrationDeadline(deadline);
        return eventRepository.save(event);
    }

    // ==================== EVENT QR CODE METHODS ====================

    /**
     * Generate QR code for event
     */
    public Event generateQRCode(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        // Generate a simple QR code string (in real implementation, this would use a QR code library)
        String qrCode = "EVENT_" + eventId.toString().replace("-", "").substring(0, 16);
        event.setQrCode(qrCode);
        event.setQrCodeEnabled(true);
        
        return eventRepository.save(event);
    }

    /**
     * Regenerate QR code for event
     */
    public Event regenerateQRCode(UUID eventId) {
        return generateQRCode(eventId);
    }

    /**
     * Disable QR code for event
     */
    public Event disableQRCode(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        event.setQrCodeEnabled(false);
        event.setQrCode(null);
        
        return eventRepository.save(event);
    }

    // ==================== EVENT VISIBILITY & ACCESS CONTROL METHODS ====================

    /**
     * Make event public
     */
    public Event makeEventPublic(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        event.setIsPublic(true);
        return eventRepository.save(event);
    }

    /**
     * Make event private
     */
    public Event makeEventPrivate(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        event.setIsPublic(false);
        return eventRepository.save(event);
    }

    /**
     * Update event visibility
     */
    public Event updateVisibility(UUID eventId, Boolean isPublic) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        event.setIsPublic(isPublic);
        return eventRepository.save(event);
    }

    // ==================== EVENT ANALYTICS METHODS ====================

    /**
     * Get event analytics overview
     */
    public Map<String, Object> getEventAnalytics(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        Map<String, Object> analytics = new java.util.HashMap<>();
        analytics.put("eventId", eventId);
        analytics.put("eventName", event.getName());
        analytics.put("currentAttendeeCount", event.getCurrentAttendeeCount());
        analytics.put("capacity", event.getCapacity());
        analytics.put("capacityUtilization", calculateCapacityUtilization(event));
        analytics.put("eventStatus", event.getEventStatus());
        analytics.put("isPublic", event.getIsPublic());
        analytics.put("qrCodeEnabled", event.getQrCodeEnabled());
        analytics.put("createdAt", event.getCreatedAt());
        analytics.put("updatedAt", event.getUpdatedAt());
        
        return analytics;
    }

    /**
     * Get attendance analytics
     */
    public Map<String, Object> getAttendanceAnalytics(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        Map<String, Object> analytics = new java.util.HashMap<>();
        analytics.put("eventId", eventId);
        analytics.put("currentAttendeeCount", event.getCurrentAttendeeCount());
        analytics.put("capacity", event.getCapacity());
        analytics.put("availableSpots", getAvailableCapacity(eventId));
        analytics.put("capacityUtilization", calculateCapacityUtilization(event));
        analytics.put("registrationDeadline", event.getRegistrationDeadline());
        analytics.put("isRegistrationOpen", isRegistrationOpen(event));
        
        return analytics;
    }

    // ==================== EVENT DUPLICATION & TEMPLATES METHODS ====================

    /**
     * Duplicate event
     */
    public Event duplicateEvent(UUID eventId, String newEventName) {
        Event originalEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        Event duplicatedEvent = new Event();
        duplicatedEvent.setName(newEventName != null ? newEventName : originalEvent.getName() + " (Copy)");
        duplicatedEvent.setDescription(originalEvent.getDescription());
        duplicatedEvent.setOwnerId(originalEvent.getOwnerId());
        duplicatedEvent.setEventType(originalEvent.getEventType());
        duplicatedEvent.setEventStatus(EventStatus.DRAFT); // Start as draft
        duplicatedEvent.setCapacity(originalEvent.getCapacity());
        duplicatedEvent.setIsPublic(false); // Start as private
        duplicatedEvent.setRequiresApproval(originalEvent.getRequiresApproval());
        duplicatedEvent.setQrCodeEnabled(false); // Disable QR code initially
        duplicatedEvent.setTheme(originalEvent.getTheme());
        duplicatedEvent.setObjectives(originalEvent.getObjectives());
        duplicatedEvent.setTargetAudience(originalEvent.getTargetAudience());
        duplicatedEvent.setSuccessMetrics(originalEvent.getSuccessMetrics());
        duplicatedEvent.setBrandingGuidelines(originalEvent.getBrandingGuidelines());
        duplicatedEvent.setVenueRequirements(originalEvent.getVenueRequirements());
        duplicatedEvent.setTechnicalRequirements(originalEvent.getTechnicalRequirements());
        duplicatedEvent.setAccessibilityFeatures(originalEvent.getAccessibilityFeatures());
        duplicatedEvent.setEmergencyPlan(originalEvent.getEmergencyPlan());
        duplicatedEvent.setBackupPlan(originalEvent.getBackupPlan());
        duplicatedEvent.setPostEventTasks(originalEvent.getPostEventTasks());
        
        return eventRepository.save(duplicatedEvent);
    }

    // ==================== EVENT VALIDATION & HEALTH CHECK METHODS ====================

    /**
     * Validate event data
     */
    public Map<String, Object> validateEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        Map<String, Object> validation = new java.util.HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Required field validations
        if (event.getName() == null || event.getName().trim().isEmpty()) {
            errors.add("Event name is required");
        }
        
        if (event.getStartDateTime() == null) {
            errors.add("Start date and time is required");
        }
        
        if (event.getEventType() == null) {
            errors.add("Event type is required");
        }
        
        // Warning validations
        if (event.getDescription() == null || event.getDescription().trim().isEmpty()) {
            warnings.add("Event description is recommended");
        }
        
        if (event.getCapacity() == null || event.getCapacity() <= 0) {
            warnings.add("Event capacity should be set");
        }
        
        if (event.getStartDateTime() != null && event.getStartDateTime().isBefore(LocalDateTime.now())) {
            warnings.add("Event start time is in the past");
        }
        
        validation.put("isValid", errors.isEmpty());
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        validation.put("eventId", eventId);
        validation.put("eventName", event.getName());
        
        return validation;
    }

    /**
     * Get event health check
     */
    public Map<String, Object> getEventHealthCheck(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        
        Map<String, Object> healthCheck = new java.util.HashMap<>();
        healthCheck.put("eventId", eventId);
        healthCheck.put("eventName", event.getName());
        healthCheck.put("status", "healthy");
        healthCheck.put("eventStatus", event.getEventStatus());
        healthCheck.put("isPublic", event.getIsPublic());
        healthCheck.put("hasQRCode", event.getQrCodeEnabled() && event.getQrCode() != null);
        healthCheck.put("hasCapacity", event.getCapacity() != null && event.getCapacity() > 0);
        healthCheck.put("hasDescription", event.getDescription() != null && !event.getDescription().trim().isEmpty());
        healthCheck.put("hasStartTime", event.getStartDateTime() != null);
        healthCheck.put("isUpcoming", event.getStartDateTime() != null && event.getStartDateTime().isAfter(LocalDateTime.now()));
        healthCheck.put("lastUpdated", event.getUpdatedAt());
        
        return healthCheck;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Convert events to user event relationship responses
     */
    private List<UserEventRelationshipResponse> convertToUserEventRelationship(List<Event> events, EventUserType userRole) {
        return events.stream()
                .map(event -> convertToUserEventRelationship(event, userRole))
                .collect(Collectors.toList());
    }

    /**
     * Convert single event to user event relationship response
     */
    private UserEventRelationshipResponse convertToUserEventRelationship(Event event, EventUserType userRole) {
        UserEventRelationshipResponse response = new UserEventRelationshipResponse();
        response.setEventId(event.getId());
        response.setEventName(event.getName());
        response.setEventDescription(event.getDescription());
        response.setEventType(event.getEventType());
        response.setEventStatus(event.getEventStatus());
        response.setStartDateTime(event.getStartDateTime());
        response.setEndDateTime(event.getEndDateTime());
        response.setUserRole(userRole);
        response.setIsOwner(userRole == EventUserType.ORGANIZER);
        response.setCapacity(event.getCapacity());
        response.setCurrentAttendeeCount(event.getCurrentAttendeeCount());
        response.setIsPublic(event.getIsPublic());
        response.setCoverImageUrl(event.getCoverImageUrl());
        response.setEventWebsiteUrl(event.getEventWebsiteUrl());
        response.setHashtag(event.getHashtag());
        
        return response;
    }

    /**
     * Calculate capacity utilization percentage
     */
    private Double calculateCapacityUtilization(Event event) {
        if (event.getCapacity() == null || event.getCapacity() == 0) {
            return null;
        }
        
        int currentCount = event.getCurrentAttendeeCount() != null ? event.getCurrentAttendeeCount() : 0;
        return (double) currentCount / event.getCapacity() * 100;
    }

    /**
     * Check if registration is open
     */
    private Boolean isRegistrationOpen(Event event) {
        if (event.getEventStatus() != EventStatus.REGISTRATION_OPEN) {
            return false;
        }
        
        if (event.getRegistrationDeadline() != null) {
            return LocalDateTime.now().isBefore(event.getRegistrationDeadline());
        }
        
        return true;
    }

}

