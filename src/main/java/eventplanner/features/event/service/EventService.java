package eventplanner.features.event.service;

import eventplanner.common.domain.enums.EventType;
import eventplanner.features.event.dto.VenueDTO;
import eventplanner.features.event.dto.request.CreateEventRequest;
import eventplanner.features.event.dto.request.UpdateEventRequest;
import eventplanner.features.event.dto.request.EventCreationRequest;
import eventplanner.features.event.dto.request.EventUpdateRequest;
import eventplanner.features.event.dto.response.EventResponse;
import eventplanner.features.event.dto.response.UserEventRelationshipResponse;
import eventplanner.features.event.dto.response.UserEventsSummaryResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.Venue;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.common.domain.enums.EventStatus;
import eventplanner.common.domain.enums.EventUserType;
import eventplanner.common.domain.enums.EventScope;
import eventplanner.security.authorization.domain.entity.EventRole;
import eventplanner.common.domain.enums.RoleName;
import eventplanner.features.event.dto.request.EventFeedRequest;
import eventplanner.features.event.dto.response.EventFeedResponse;
import eventplanner.features.event.dto.response.FeedPost;
import java.util.Set;
import java.util.Comparator;
import eventplanner.security.authorization.domain.repository.EventRoleRepository;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventService {

    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private EventRoleRepository eventRoleRepository;
    
    @Autowired(required = false)
    private AuthorizationService authorizationService;
    
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
                    event.setEventType(eventplanner.common.domain.enums.EventType.valueOf(request.getEventType()));
                } catch (IllegalArgumentException e) {
                    event.setEventType(eventplanner.common.domain.enums.EventType.MEETING);
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
            
            // Set venue if provided
            if (request.getVenue() != null) {
                event.setVenue(toVenueEntity(request.getVenue()));
            }
            
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
            
            // Update venue if provided
            if (request.getVenue() != null) {
                event.setVenue(toVenueEntity(request.getVenue()));
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
     * Get event by ID with access control check
     * Only returns the event if the user has permission to access it
     * @param id The event ID
     * @param user The user principal (can be null for public access checks)
     * @return Optional containing the event if found and accessible
     */
    public Optional<Event> getByIdWithAccessControl(UUID id, UserPrincipal user) {
        Optional<Event> eventOpt = eventRepository.findById(id);
        if (eventOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Event event = eventOpt.get();
        
        // If authorization service is available and user is provided, check access
        if (authorizationService != null && user != null) {
            if (!authorizationService.canAccessEvent(user, id)) {
                return Optional.empty();
            }
        } else if (user == null) {
            // No user context - only return public events
            if (!Boolean.TRUE.equals(event.getIsPublic())) {
                return Optional.empty();
            }
        }
        
        return Optional.of(event);
    }

    /**
     * Get current user from security context
     */
    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Filter events based on user access
     * Returns only events that are either public OR owned by user OR user has role in
     */
    private List<Event> filterEventsByAccess(List<Event> events, UserPrincipal user) {
        if (user == null || authorizationService == null) {
            // No user context - only return public events
            return events.stream()
                .filter(event -> Boolean.TRUE.equals(event.getIsPublic()))
                .collect(Collectors.toList());
        }
        
        return events.stream()
            .filter(event -> {
                if (Boolean.TRUE.equals(event.getIsPublic())) {
                    return true; // Public events are accessible
                }
                // For private events, check if user has access
                return authorizationService.canAccessEvent(user, event.getId());
            })
            .collect(Collectors.toList());
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
        if (request.isVenueCleared()) {
            event.setVenueId(null);
            event.setVenue(null);
        }
        
        // Update venue if provided
        if (request.getVenue() != null) {
            event.setVenue(toVenueEntity(request.getVenue()));
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
        event.setEventStatus(request.getEventStatus() != null ? request.getEventStatus() : EventStatus.PLANNING);
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setCapacity(request.getCapacity());
        event.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : Boolean.TRUE);
        event.setRequiresApproval(request.getRequiresApproval() != null ? request.getRequiresApproval() : Boolean.FALSE);
        event.setVenueRequirements(request.getVenueRequirements());
        event.setCoverImageUrl(request.getCoverImageUrl());
        event.setEventWebsiteUrl(request.getEventWebsiteUrl());
        event.setHashtag(request.getHashtag());
        event.setTheme(request.getTheme());
        event.setObjectives(request.getObjectives());
        event.setTargetAudience(request.getTargetAudience());
        event.setSuccessMetrics(request.getSuccessMetrics());
        event.setBrandingGuidelines(request.getBrandingGuidelines());
        event.setTechnicalRequirements(request.getTechnicalRequirements());
        event.setAccessibilityFeatures(request.getAccessibilityFeatures());
        event.setEmergencyPlan(request.getEmergencyPlan());
        event.setBackupPlan(request.getBackupPlan());
        event.setPostEventTasks(request.getPostEventTasks());
        event.setMetadata(request.getMetadata());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setCurrentAttendeeCount(request.getCurrentAttendeeCount() != null ? request.getCurrentAttendeeCount() : 0);
        
        // Set venue if provided
        if (request.getVenue() != null) {
            event.setVenue(toVenueEntity(request.getVenue()));
        }
        
        Event savedEvent = eventRepository.save(event);
        assignOwnerOrganizerRole(savedEvent.getId(), ownerId);
        return savedEvent;
    }

    /**
     * List events with pagination and filtering
     * @param request The list request with filters
     * @param user The user principal for access control
     * @return Page of events
     */
    public Page<Event> listEvents(eventplanner.features.event.dto.request.EventListRequest request, UserPrincipal user) {
        // Enforce max page size
        int page = Math.max(0, request.getPage() != null ? request.getPage() : 0);
        int size = Math.min(100, Math.max(1, request.getSize() != null ? request.getSize() : 20));
        
        // Build sort
        Sort.Direction direction = "DESC".equalsIgnoreCase(request.getSortDirection()) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, request.getSortBy() != null ? request.getSortBy() : "startDateTime");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Normalize search term
        // IMPORTANT: Do not pass null into JPQL optional-search predicates.
        // Postgres can fail to infer the parameter type when the same null parameter
        // is used in multiple expressions (e.g. LOWER(CONCAT('%', :search, '%'))),
        // resulting in "could not determine data type of parameter $N".
        String search = request.getSearch() != null ? request.getSearch().trim() : "";
        
        // Default to non-archived events unless explicitly requested
        Boolean isArchived = request.getIsArchived() != null ? request.getIsArchived() : false;
        
        // Note: Owner scoping is handled in-memory for now (mine=true).
        // We keep using the shared query and apply owner filtering after fetch.

        // Optional timeframe filtering (relative to now)
        LocalDateTime startDateFrom = request.getStartDateFrom();
        LocalDateTime startDateTo = request.getStartDateTo();
        if (request.getTimeframe() != null && !request.getTimeframe().trim().isEmpty()) {
            String tf = request.getTimeframe().trim().toUpperCase(Locale.US);
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            if ("UPCOMING".equals(tf)) {
                if (startDateFrom == null || startDateFrom.isBefore(now)) {
                    startDateFrom = now;
                }
            } else if ("PAST".equals(tf)) {
                if (startDateTo == null || startDateTo.isAfter(now)) {
                    startDateTo = now;
                }
            } else {
                throw new IllegalArgumentException("Invalid timeframe. Use UPCOMING or PAST.");
            }
        }

        // Public discovery safety: if no authenticated user context, force public-only results.
        Boolean isPublic = request.getIsPublic();
        if (user == null) {
            isPublic = true;
        }

        final Boolean publicFilter = isPublic;
        final LocalDateTime startFrom = startDateFrom;
        final LocalDateTime startTo = startDateTo;

        // Build dynamic criteria instead of "(:param is null or ...)" JPQL.
        // Postgres can fail to infer parameter types for nulls in those patterns ("could not determine data type of parameter $N").
        Specification<Event> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("eventStatus"), request.getStatus()));
            }
            if (request.getEventType() != null) {
                predicates.add(cb.equal(root.get("eventType"), request.getEventType()));
            }
            if (publicFilter != null) {
                predicates.add(cb.equal(root.get("isPublic"), publicFilter));
            }
            if (startFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDateTime"), startFrom));
            }
            if (startTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDateTime"), startTo));
            }
            if (isArchived != null) {
                predicates.add(cb.equal(root.get("isArchived"), isArchived));
            }

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                jakarta.persistence.criteria.Expression<String> name = cb.lower(root.get("name"));
                jakarta.persistence.criteria.Expression<String> description = cb.lower(root.get("description"));
                jakarta.persistence.criteria.Expression<String> hashtag = cb.lower(root.get("hashtag"));
                jakarta.persistence.criteria.Expression<String> theme = cb.lower(root.get("theme"));

                predicates.add(cb.or(
                        cb.like(name, like),
                        cb.like(description, like),
                        cb.like(hashtag, like),
                        cb.like(theme, like)
                ));
            }

            if (Boolean.TRUE.equals(request.getMine()) && user != null) {
                predicates.add(cb.equal(root.get("ownerId"), user.getId()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Event> events = eventRepository.findAll(spec, pageable);

        // Apply mine=true filter (owned-by-current-user) as a post-filter.
        // This avoids exposing arbitrary ownerId querying.
        if (Boolean.TRUE.equals(request.getMine()) && user != null) {
            List<Event> owned = events.getContent().stream()
                    .filter(e -> e.getOwnerId() != null && e.getOwnerId().equals(user.getId()))
                    .collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(owned, pageable, owned.size());
        }
        
        // Filter by access control if user is provided
        if (user != null && authorizationService != null) {
            // Filter events based on user access
            List<Event> accessibleEvents = events.getContent().stream()
                    .filter(event -> authorizationService.canAccessEvent(user, event.getId()))
                    .collect(Collectors.toList());
            
            // Recreate page with filtered content
            return new org.springframework.data.domain.PageImpl<>(
                    accessibleEvents,
                    pageable,
                    accessibleEvents.size()
            );
        }
        
        return events;
    }

    /**
     * Archive an event (soft delete)
     * @param id The event ID
     * @param archivedBy The user ID archiving the event
     * @param reason Optional reason for archiving
     * @return Archived event
     */
    public Event archiveEvent(UUID id, UUID archivedBy, String reason) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + id));
        
        if (Boolean.TRUE.equals(event.getIsArchived())) {
            throw new IllegalArgumentException("Event is already archived");
        }
        
        event.setIsArchived(true);
        event.setArchivedAt(LocalDateTime.now());
        event.setArchivedBy(archivedBy);
        event.setArchiveReason(reason);
        
        return eventRepository.save(event);
    }

    /**
     * Restore an archived event
     * @param id The event ID
     * @param restoredBy The user ID restoring the event
     * @return Restored event
     */
    public Event restoreEvent(UUID id, UUID restoredBy) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + id));
        
        if (!Boolean.TRUE.equals(event.getIsArchived())) {
            throw new IllegalArgumentException("Event is not archived");
        }
        
        event.setIsArchived(false);
        event.setRestoredAt(LocalDateTime.now());
        event.setRestoredBy(restoredBy);
        // Clear archive fields but keep for audit trail
        // event.setArchivedAt(null);
        // event.setArchivedBy(null);
        // event.setArchiveReason(null);
        
        return eventRepository.save(event);
    }

    /**
     * Update event with optimistic locking check
     * @param id The event ID
     * @param request The update request
     * @param expectedVersion The expected version from If-Match header
     * @return Updated event
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if version mismatch
     */
    public Event updateWithVersion(UUID id, UpdateEventRequest request, Long expectedVersion) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + id));
        
        // Check version if provided
        if (expectedVersion != null && !expectedVersion.equals(event.getVersion())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(
                    "Event has been modified by another user. Current version: " + event.getVersion(),
                    event.getClass()
            );
        }
        
        // Update fields (same as regular update)
        return update(id, request);
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
        response.setRegistrationDeadline(event.getRegistrationDeadline());
        response.setCapacity(event.getCapacity());
        response.setCurrentAttendeeCount(event.getCurrentAttendeeCount());
        response.setIsPublic(event.getIsPublic());
        response.setRequiresApproval(event.getRequiresApproval());
        response.setEventWebsiteUrl(event.getEventWebsiteUrl());
        response.setHashtag(event.getHashtag());
        response.setTheme(event.getTheme());
        response.setObjectives(event.getObjectives());
        response.setTargetAudience(event.getTargetAudience());
        response.setSuccessMetrics(event.getSuccessMetrics());
        response.setBrandingGuidelines(event.getBrandingGuidelines());
        response.setVenueRequirements(event.getVenueRequirements());
        response.setTechnicalRequirements(event.getTechnicalRequirements());
        response.setAccessibilityFeatures(event.getAccessibilityFeatures());
        response.setEmergencyPlan(event.getEmergencyPlan());
        response.setBackupPlan(event.getBackupPlan());
        response.setPostEventTasks(event.getPostEventTasks());
        response.setMetadata(event.getMetadata());
        response.setOwnerId(event.getOwnerId());
        response.setVenueId(event.getVenueId());
        response.setVenue(event.getVenue() != null ? toVenueDTO(event.getVenue()) : null);
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        response.setScope(EventScope.FULL); // Full details response
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
        // QR code fields removed
        
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
        // QR code fields removed
        
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

    private void assignOwnerOrganizerRole(UUID eventId, UUID ownerId) {
        if (eventId == null || ownerId == null) {
            return;
        }
        
        Optional<EventRole> existingRole = eventRoleRepository
                .findByEventIdAndUserIdAndRoleName(eventId, ownerId, RoleName.ORGANIZER);
        
        if (existingRole.isPresent()) {
            EventRole role = existingRole.get();
            if (Boolean.FALSE.equals(role.getIsActive())) {
                role.setIsActive(true);
                role.setAssignedAt(LocalDateTime.now());
                role.setAssignedBy(ownerId);
                eventRoleRepository.save(role);
            }
            return;
        }
        
        EventRole organizerRole = new EventRole();
        organizerRole.setEventId(eventId);
        organizerRole.setUserId(ownerId);
        organizerRole.setRoleName(RoleName.ORGANIZER);
        organizerRole.setIsActive(true);
        organizerRole.setAssignedBy(ownerId);
        organizerRole.setAssignedAt(LocalDateTime.now());
        organizerRole.setNotes("Auto-assigned event owner");
        eventRoleRepository.save(organizerRole);
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
        List<Event> events = eventRepository.searchEvents(searchTerm.trim());
        // Filter by access - only return events user can access
        UserPrincipal user = getCurrentUser();
        return filterEventsByAccess(events, user);
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
        try {
            EventType type = EventType.valueOf(eventType.toUpperCase(Locale.US));
            List<Event> events = eventRepository.findByEventType(type);
            // Filter by access - only return events user can access
            UserPrincipal user = getCurrentUser();
            return filterEventsByAccess(events, user);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid event type: " + eventType);
        }
    }

    /**
     * Get events by status
     */
    public List<Event> getEventsByStatus(String eventStatus) {
        try {
            EventStatus status = EventStatus.valueOf(eventStatus.toUpperCase(Locale.US));
            List<Event> events = eventRepository.findByEventStatus(status);
            // Filter by access - only return events user can access
            UserPrincipal user = getCurrentUser();
            return filterEventsByAccess(events, user);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid event status: " + eventStatus);
        }
    }

    /**
     * Get events by date range
     */
    public List<Event> getEventsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Event> events = eventRepository.findByStartDateTimeBetween(startDate, endDate);
        // Filter by access - only return events user can access
        UserPrincipal user = getCurrentUser();
        return filterEventsByAccess(events, user);
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
        
        // Copy venue
        duplicatedEvent.setVenueId(originalEvent.getVenueId());
        if (originalEvent.getVenue() != null) {
            // Create a new Venue instance with copied values
            Venue copiedVenue = new Venue();
            Venue originalVenue = originalEvent.getVenue();
            copiedVenue.setAddress(originalVenue.getAddress());
            copiedVenue.setCity(originalVenue.getCity());
            copiedVenue.setState(originalVenue.getState());
            copiedVenue.setCountry(originalVenue.getCountry());
            copiedVenue.setZipCode(originalVenue.getZipCode());
            copiedVenue.setLatitude(originalVenue.getLatitude());
            copiedVenue.setLongitude(originalVenue.getLongitude());
            copiedVenue.setGooglePlaceId(originalVenue.getGooglePlaceId());
            copiedVenue.setGooglePlaceData(originalVenue.getGooglePlaceData());
            duplicatedEvent.setVenue(copiedVenue);
        }
        
        return eventRepository.save(duplicatedEvent);
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

    // (capacity utilization / registration-open helpers removed with analytics endpoints)

    /**
     * Convert VenueDTO to Venue entity
     */
    private Venue toVenueEntity(VenueDTO dto) {
        if (dto == null) {
            return null;
        }
        Venue venue = new Venue();
        venue.setAddress(dto.getAddress());
        venue.setCity(dto.getCity());
        venue.setState(dto.getState());
        venue.setCountry(dto.getCountry());
        venue.setZipCode(dto.getZipCode());
        venue.setLatitude(dto.getLatitude());
        venue.setLongitude(dto.getLongitude());
        venue.setGooglePlaceId(dto.getGooglePlaceId());
        venue.setGooglePlaceData(dto.getGooglePlaceData());
        return venue;
    }

    /**
     * Convert Venue entity to VenueDTO
     */
    private VenueDTO toVenueDTO(Venue venue) {
        if (venue == null) {
            return null;
        }
        VenueDTO dto = new VenueDTO();
        dto.setAddress(venue.getAddress());
        dto.setCity(venue.getCity());
        dto.setState(venue.getState());
        dto.setCountry(venue.getCountry());
        dto.setZipCode(venue.getZipCode());
        dto.setLatitude(venue.getLatitude());
        dto.setLongitude(venue.getLongitude());
        dto.setGooglePlaceId(venue.getGooglePlaceId());
        dto.setGooglePlaceData(venue.getGooglePlaceData());
        return dto;
    }

    // ==================== EVENT SCOPE & FEED METHODS ====================

    /**
     * Determine the event scope for a user based on their relationship to the event
     * @param user The user principal
     * @param eventId The event ID
     * @return EventScope - FULL for owners/high-responsibility, FEED for guests
     */
    public EventScope determineEventScope(UserPrincipal user, UUID eventId) {
        if (user == null || eventId == null) {
            return EventScope.FEED; // Default to feed for unauthenticated
        }
        
        // Owners always get full scope
        if (authorizationService != null && authorizationService.isEventOwner(user, eventId)) {
            return EventScope.FULL;
        }
        
        // Check user's role in the event
        List<EventRole> roles = eventRoleRepository.findByEventIdAndUserId(eventId, user.getId());
        
        // High-responsibility roles get full scope
        Set<RoleName> highResponsibilityRoles = Set.of(
            RoleName.ORGANIZER, 
            RoleName.COORDINATOR,
            RoleName.STAFF
        );
        
        boolean hasHighResponsibility = roles.stream()
            .filter(EventRole::getIsActive)
            .map(EventRole::getRoleName)
            .anyMatch(highResponsibilityRoles::contains);
        
        if (hasHighResponsibility) {
            return EventScope.FULL;
        }
        
        // Everyone else (GUEST, attendees, etc.) gets feed view
        return EventScope.FEED;
    }

    /**
     * Convert event to feed response for guest users with pagination
     * @param event The event entity
     * @param user The user principal (optional)
     * @param request The feed request with pagination parameters
     * @return EventFeedResponse with paginated posts
     */
    public EventFeedResponse toFeedResponse(Event event, UserPrincipal user, EventFeedRequest request) {
        EventFeedResponse feed = new EventFeedResponse();
        
        // Basic event info (public-facing only)
        feed.setEventId(event.getId());
        feed.setEventName(event.getName());
        feed.setDescription(event.getDescription());
        feed.setCoverImageUrl(event.getCoverImageUrl());
        feed.setStartDateTime(event.getStartDateTime());
        feed.setEndDateTime(event.getEndDateTime());
        feed.setHashtag(event.getHashtag());
        feed.setEventWebsiteUrl(event.getEventWebsiteUrl());
        
        // Build pagination
        int page = request != null && request.getPage() != null ? request.getPage() : 0;
        int size = request != null && request.getSize() != null ? request.getSize() : 20;
        size = Math.min(50, Math.max(1, size)); // Enforce max page size
        
        // Aggregate all posts from various sources
        List<FeedPost> allPosts = aggregateAllFeedPosts(event, user, request);
        
        // Calculate pagination
        long totalPosts = allPosts.size();
        int totalPages = (int) Math.ceil((double) totalPosts / size);
        int start = page * size;
        int end = Math.min(start + size, allPosts.size());
        
        // Get paginated subset
        List<FeedPost> paginatedPosts = start < allPosts.size() 
            ? allPosts.subList(start, end) 
            : new ArrayList<>();
        
        // Set posts and pagination metadata
        feed.setPosts(paginatedPosts);
        feed.setCurrentPage(page);
        feed.setPageSize(size);
        feed.setTotalPosts(totalPosts);
        feed.setTotalPages(totalPages);
        feed.setHasNext(page < totalPages - 1);
        feed.setHasPrevious(page > 0);
        feed.setScope(EventScope.FEED); // Feed view response
        
        return feed;
    }

    /**
     * Overloaded method for backward compatibility (no pagination)
     */
    public EventFeedResponse toFeedResponse(Event event, UserPrincipal user) {
        EventFeedRequest request = new EventFeedRequest();
        request.setPage(0);
        request.setSize(20);
        return toFeedResponse(event, user, request);
    }

    /**
     * Aggregate all feed posts from internal sources only
     * @param event The event entity
     * @param user The user principal
     * @param request The feed request with filters
     * @return List of all feed posts (before pagination)
     */
    private List<FeedPost> aggregateAllFeedPosts(Event event, UserPrincipal user, EventFeedRequest request) {
        List<FeedPost> posts = new ArrayList<>();
        
        // TODO: Implement actual feed aggregation
        // 1. Get event media and convert to posts
        // 2. Get user-generated content from internal app
        
        // Apply filters if specified
        if (request != null && request.getPostType() != null && !request.getPostType().equals("ALL")) {
            posts = posts.stream()
                .filter(post -> request.getPostType().equalsIgnoreCase(post.getType()))
                .collect(Collectors.toList());
        }
        
        // Sort by posted date (newest first)
        posts.sort(Comparator.comparing(FeedPost::getPostedAt, 
            Comparator.nullsLast(Comparator.reverseOrder())));
        
        return posts;
    }

    /**
     * Build event feed with pagination (always returns feed, regardless of user role)
     * This is for the separate /feed endpoint
     * @param eventId The event ID
     * @param user The user principal
     * @param request The feed request with pagination
     * @return EventFeedResponse with paginated posts
     */
    public EventFeedResponse buildEventFeed(UUID eventId, UserPrincipal user, EventFeedRequest request) {
        Optional<Event> eventOpt = getByIdWithAccessControl(eventId, user);
        if (eventOpt.isEmpty()) {
            throw new IllegalArgumentException("Event not found or access denied");
        }
        
        return toFeedResponse(eventOpt.get(), user, request);
    }

}
