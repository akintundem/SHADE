package eventplanner.features.event.service;

import eventplanner.features.event.dto.VenueDTO;
import eventplanner.features.event.dto.request.CreateEventRequest;
import eventplanner.features.event.dto.request.UpdateEventRequest;
import eventplanner.features.event.dto.request.EventListRequest;
import eventplanner.features.event.dto.request.CloneEventRequest;
import eventplanner.features.event.dto.response.EventResponse;
import eventplanner.features.event.dto.response.TicketTypeSummary;
import eventplanner.features.event.dto.response.UserEventContext;
import eventplanner.features.event.entity.Event;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.entity.Ticket;
import eventplanner.features.ticket.enums.TicketStatus;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.event.entity.Venue;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.enums.CommunicationType;
import eventplanner.common.config.ExternalServicesProperties;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.event.enums.EventAccessType;
import eventplanner.features.event.enums.EventStatus;
import eventplanner.features.event.enums.EventScope;
import eventplanner.features.event.entity.EventRole;
import eventplanner.features.event.enums.RoleName;
import eventplanner.features.event.dto.request.EventFeedRequest;
import eventplanner.features.event.dto.response.EventFeedResponse;
import eventplanner.features.event.dto.response.FeedPost;
import eventplanner.features.feeds.entity.EventFeedPost;
import eventplanner.features.feeds.repository.FeedPostRepository;
import eventplanner.features.event.entity.EventStoredObject;
import eventplanner.features.event.repository.EventStoredObjectRepository;
import eventplanner.features.social.repository.UserFollowRepository;
import eventplanner.features.budget.service.BudgetService;
import eventplanner.common.storage.s3.registry.BucketAlias;
import eventplanner.common.storage.s3.services.S3StorageService;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.common.communication.services.core.BulkNotificationService;
import eventplanner.common.communication.services.core.NotificationTargetResolver;
import eventplanner.common.communication.services.core.dto.BulkNotificationRequest;
import eventplanner.common.communication.services.core.dto.NotificationTarget;
import java.util.Set;
import eventplanner.features.event.repository.EventRoleRepository;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.common.exception.exceptions.ForbiddenException;
import eventplanner.common.exception.exceptions.UnauthorizedException;
import eventplanner.common.exception.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

@Service
@Transactional
@Slf4j
public class EventService {

    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private EventRoleRepository eventRoleRepository;
    
    @Autowired(required = false)
    private FeedPostRepository eventPostRepository;

    @Autowired(required = false)
    private EventStoredObjectRepository eventStoredObjectRepository;

    @Autowired(required = false)
    private S3StorageService s3StorageService;

    @Autowired(required = false)
    private UserAccountRepository userAccountRepository;

    @Autowired(required = false)
    private AuthorizationService authorizationService;
    
    @Autowired
    private BudgetService budgetService;
    
    @Autowired(required = false)
    private UserEventContextService userEventContextService;

    @Autowired(required = false)
    private TicketTypeRepository ticketTypeRepository;
    
    @Autowired(required = false)
    private eventplanner.features.ticket.service.TicketTypeService ticketTypeService;
    
    @Autowired(required = false)
    private eventplanner.features.event.service.EventWaitlistService eventWaitlistService;

    @Autowired(required = false)
    private TicketRepository ticketRepository;

    @Autowired(required = false)
    private NotificationService notificationService;
    
    @Autowired(required = false)
    private AttendeeRepository attendeeRepository;

    @Autowired(required = false)
    private ExternalServicesProperties externalServicesProperties;

    @Autowired
    private NotificationTargetResolver notificationTargetResolver;

    @Autowired
    private BulkNotificationService bulkNotificationService;

    @Autowired(required = false)
    private UserFollowRepository userFollowRepository;

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
        } else {
            // Authorization service unavailable; allow owner, otherwise require public
            boolean isOwner = event.getOwner() != null && event.getOwner().getId().equals(user.getId());
            if (!isOwner && !Boolean.TRUE.equals(event.getIsPublic())) {
                return Optional.empty();
            }
        }
        
        return Optional.of(event);
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
        boolean importantChange = false;
        List<String> changeSummary = new ArrayList<>();
        
        // Update only the fields that are provided (not null)
        if (request.getName() != null) {
            if (!Objects.equals(event.getName(), request.getName())) {
                importantChange = true;
                changeSummary.add("Name");
            }
            event.setName(request.getName());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventType() != null) {
            event.setEventType(request.getEventType());
        }
        if (request.getEventStatus() != null) {
            if (!Objects.equals(event.getEventStatus(), request.getEventStatus())) {
                importantChange = true;
                changeSummary.add("Status");
            }
            event.setEventStatus(request.getEventStatus());
        }
        if (request.getStartDateTime() != null) {
            if (!Objects.equals(event.getStartDateTime(), request.getStartDateTime())) {
                importantChange = true;
                changeSummary.add("Start date/time");
            }
            event.setStartDateTime(request.getStartDateTime());
        }
        if (request.getEndDateTime() != null) {
            if (!Objects.equals(event.getEndDateTime(), request.getEndDateTime())) {
                importantChange = true;
                changeSummary.add("End date/time");
            }
            event.setEndDateTime(request.getEndDateTime());
        }
        if (request.getRegistrationDeadline() != null) {
            if (!Objects.equals(event.getRegistrationDeadline(), request.getRegistrationDeadline())) {
                importantChange = true;
                changeSummary.add("Registration deadline");
            }
            event.setRegistrationDeadline(request.getRegistrationDeadline());
        }
        if (request.getCapacity() != null) {
            Integer oldCapacity = event.getCapacity();
            Integer newCapacity = request.getCapacity();
            if (!Objects.equals(oldCapacity, newCapacity)) {
                importantChange = true;
                changeSummary.add("Capacity");

                // Validate capacity decrease
                if (oldCapacity != null && newCapacity < oldCapacity) {
                    Integer currentCount = event.getCurrentAttendeeCount() != null ? event.getCurrentAttendeeCount() : 0;
                    if (currentCount > newCapacity) {
                        throw new BadRequestException(
                            String.format("Cannot reduce capacity to %d when %d attendees are already confirmed",
                                newCapacity, currentCount));
                    }
                }
            }
            event.setCapacity(newCapacity);

            // Promote waitlist entries if capacity increased
            if (eventWaitlistService != null && oldCapacity != null && newCapacity != null && newCapacity > oldCapacity) {
                try {
                    Integer currentCount = event.getCurrentAttendeeCount() != null ? event.getCurrentAttendeeCount() : 0;
                    // Fixed calculation: spots available = new capacity - current confirmed attendees
                    int spotsAvailable = newCapacity - currentCount;
                    if (spotsAvailable > 0) {
                        eventWaitlistService.promoteWaitlistEntries(event.getId(), spotsAvailable);
                    }
                } catch (Exception e) {
                    // Log but don't fail the update
                    log.warn("Failed to promote waitlist entries when capacity increased for event {}: {}",
                            event.getId(), e.getMessage());
                }
            }
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
        
        // Update access control settings
        if (request.getAccessType() != null) {
            EventAccessType oldAccessType = event.getAccessType();
            EventAccessType newAccessType = request.getAccessType();
            
            // Validate access type change
            if (!oldAccessType.equals(newAccessType)) {
                validateAccessTypeChange(event, oldAccessType, newAccessType);
            }
            
            event.setAccessType(newAccessType);
        }
        if (request.getFeedsPublicAfterEvent() != null) {
            event.setFeedsPublicAfterEvent(request.getFeedsPublicAfterEvent());
        }
        
        Event saved = eventRepository.save(event);
        if (importantChange) {
            notifyEventUpdate(saved, changeSummary);
        }
        return saved;
    }
    
    private void notifyEventUpdate(Event event, List<String> changeSummary) {
        try {
            if (event == null || event.getId() == null) {
                return;
            }
            
            Set<UUID> recipientIds = new HashSet<>();
            
            NotificationTarget attendees = notificationTargetResolver.resolveTarget(
                    NotificationTarget.TargetType.EVENT_ATTENDEES,
                    event.getId(),
                    Map.of());
            if (attendees.getUserIds() != null) {
                recipientIds.addAll(attendees.getUserIds());
            }
            
            NotificationTarget collaborators = notificationTargetResolver.resolveTarget(
                    NotificationTarget.TargetType.EVENT_COLLABORATORS,
                    event.getId(),
                    Map.of());
            if (collaborators.getUserIds() != null) {
                recipientIds.addAll(collaborators.getUserIds());
            }
            
            if (event.getOwner() != null && event.getOwner().getId() != null) {
                recipientIds.add(event.getOwner().getId());
            }
            
            if (recipientIds.isEmpty()) {
                return;
            }
            
            String summaryText = changeSummary != null && !changeSummary.isEmpty()
                    ? "Updated: " + String.join(", ", changeSummary)
                    : "Event details updated";
            String title = event.getName() != null && !event.getName().isBlank()
                    ? "Event updated: " + event.getName()
                    : "Event updated";
            
            Map<String, String> data = new HashMap<>();
            data.put("eventId", event.getId().toString());
            data.put("changeSummary", summaryText);
            data.put("type", "event_updated");
            if (event.getName() != null && !event.getName().isBlank()) {
                data.put("eventName", event.getName());
            }
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("userIds", new ArrayList<>(recipientIds));
            
            BulkNotificationRequest request = BulkNotificationRequest.builder()
                    .targetType(NotificationTarget.TargetType.SPECIFIC_USERS)
                    .eventId(event.getId())
                    .title(title)
                    .body(summaryText)
                    .data(data)
                    .parameters(parameters)
                    .build();

            // CRITICAL: Event updates MUST be communicated to subscribers
            // (time changes, location changes, cancellations, etc.)
            // If notification fails, roll back the update to avoid confusion
            bulkNotificationService.sendBulkPushNotification(request);
        } catch (Exception e) {
            // Log error with full context
            log.error("CRITICAL: Failed to notify subscribers about event update for event {}: {}",
                    event.getId(), e.getMessage(), e);
            // Re-throw to cause transaction rollback
            throw new IllegalStateException(
                    "Failed to notify event subscribers about update. " +
                    "Event changes rolled back to prevent inconsistency.", e);
        }
    }

    /**
     * Create event from CreateEventRequest
     * @param request The create event request
     * @param ownerId The owner ID
     * @return Created event
     */
    public Event create(CreateEventRequest request, UUID ownerId) {
        if (ownerId == null) {
            throw new BadRequestException("Owner ID is required for event creation");
        }

        UserAccount owner = userAccountRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
        
        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setOwner(owner);
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
        
        // Set access control settings
        event.setAccessType(request.getAccessType() != null ? request.getAccessType() : EventAccessType.OPEN);
        event.setFeedsPublicAfterEvent(request.getFeedsPublicAfterEvent() != null ? request.getFeedsPublicAfterEvent() : Boolean.FALSE);
        
        Event savedEvent = eventRepository.save(event);
        assignOwnerOrganizerRole(savedEvent.getId(), ownerId);
        
        // Auto-create budget for the event
        budgetService.createInitialBudget(savedEvent, owner);
        
        return savedEvent;
    }

    /**
     * Clone an existing event.
     * Creates a new event with copied settings from the source event.
     * 
     * @param sourceEventId The ID of the event to clone
     * @param request Clone request with optional overrides
     * @param principal User principal for access control and ownership
     * @return Cloned event
     */
    @Transactional
    public Event cloneEvent(UUID sourceEventId, CloneEventRequest request, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new UnauthorizedException("Authentication required to clone events");
        }

        // Fetch source event
        Event source = eventRepository.findById(sourceEventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + sourceEventId));

        // Check access - user must be able to access the source event
        if (authorizationService != null && !authorizationService.canAccessEvent(principal, sourceEventId)) {
            throw new ForbiddenException("Access denied to event: " + sourceEventId);
        }

        // Create new event
        Event clone = new Event();
        
        // Set owner to current user
        UserAccount owner = userAccountRepository.findById(principal.getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        clone.setOwner(owner);

        // Set name
        String cloneName = request != null && request.getName() != null && !request.getName().isBlank()
            ? request.getName().trim()
            : source.getName() + " (Copy)";
        clone.setName(cloneName);

        // Copy basic event information
        clone.setDescription(source.getDescription());
        clone.setEventType(source.getEventType());
        clone.setEventStatus(request != null && request.getEventStatus() != null 
            ? request.getEventStatus() 
            : EventStatus.DRAFT);
        clone.setHashtag(source.getHashtag());
        clone.setTheme(source.getTheme());
        clone.setObjectives(source.getObjectives());
        clone.setTargetAudience(source.getTargetAudience());
        clone.setSuccessMetrics(source.getSuccessMetrics());
        clone.setBrandingGuidelines(source.getBrandingGuidelines());
        clone.setTechnicalRequirements(source.getTechnicalRequirements());
        clone.setAccessibilityFeatures(source.getAccessibilityFeatures());
        clone.setEmergencyPlan(source.getEmergencyPlan());
        clone.setBackupPlan(source.getBackupPlan());
        clone.setPostEventTasks(source.getPostEventTasks());
        clone.setMetadata(source.getMetadata());
        clone.setVenueRequirements(source.getVenueRequirements());
        clone.setEventWebsiteUrl(source.getEventWebsiteUrl());

        // Set dates (use provided or original)
        clone.setStartDateTime(request != null && request.getStartDateTime() != null
            ? request.getStartDateTime()
            : source.getStartDateTime());
        clone.setEndDateTime(request != null && request.getEndDateTime() != null
            ? request.getEndDateTime()
            : source.getEndDateTime());
        clone.setRegistrationDeadline(source.getRegistrationDeadline());

        // Copy access control settings
        clone.setAccessType(source.getAccessType());
        clone.setIsPublic(source.getIsPublic());
        clone.setRequiresApproval(source.getRequiresApproval());
        clone.setFeedsPublicAfterEvent(source.getFeedsPublicAfterEvent());

        // Copy capacity (reset attendee count)
        clone.setCapacity(source.getCapacity());
        clone.setCurrentAttendeeCount(0);

        // Copy venue if requested
        if (request == null || Boolean.TRUE.equals(request.getCloneVenue())) {
            if (source.getVenue() != null) {
                Venue venue = new Venue();
                venue.setAddress(source.getVenue().getAddress());
                venue.setCity(source.getVenue().getCity());
                venue.setState(source.getVenue().getState());
                venue.setCountry(source.getVenue().getCountry());
                venue.setZipCode(source.getVenue().getZipCode());
                venue.setLatitude(source.getVenue().getLatitude());
                venue.setLongitude(source.getVenue().getLongitude());
                venue.setGooglePlaceId(source.getVenue().getGooglePlaceId());
                venue.setGooglePlaceData(source.getVenue().getGooglePlaceData());
                clone.setVenue(venue);
            }
            clone.setVenueId(source.getVenueId());
        }

        // Copy cover image URL (not the actual file, just the reference)
        clone.setCoverImageUrl(source.getCoverImageUrl());

        // Save the cloned event
        Event savedClone = eventRepository.save(clone);

        // Assign owner as organizer
        assignOwnerOrganizerRole(savedClone.getId(), principal.getId());

        // Auto-create budget for the cloned event
        budgetService.createInitialBudget(savedClone, owner);

        // Clone ticket types if requested
        if (request != null && Boolean.TRUE.equals(request.getCloneTicketTypes()) && ticketTypeService != null) {
            List<TicketType> sourceTicketTypes = ticketTypeRepository.findByEventIdAndIsActiveTrue(sourceEventId);
            for (TicketType sourceTicketType : sourceTicketTypes) {
                try {
                    eventplanner.features.ticket.dto.request.CloneTicketTypeRequest cloneTicketRequest = 
                        new eventplanner.features.ticket.dto.request.CloneTicketTypeRequest();
                    cloneTicketRequest.setName(null); // Use default naming
                    cloneTicketRequest.setIsActive(false); // Start inactive
                    ticketTypeService.cloneTicketType(sourceTicketType.getId(), savedClone.getId(), cloneTicketRequest, principal);
                } catch (Exception e) {
                    // Log but don't fail the entire clone operation
                    // Ticket type cloning is best-effort
                }
            }
        }

        // Note: Media/assets cloning is not implemented as it would require copying S3 objects
        // Users can manually upload media to the cloned event if needed

        return savedClone;
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
                throw new BadRequestException("Invalid timeframe. Use UPCOMING or PAST.");
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
        final UUID currentUserId = user != null ? user.getId() : null;
        final boolean authAvailable = authorizationService != null;

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
            // If no authorization service, restrict to public or owned by current user to avoid leaking private events.
            if (!authAvailable) {
                jakarta.persistence.criteria.Predicate isPublicPredicate = cb.equal(root.get("isPublic"), true);
                if (currentUserId != null) {
                    jakarta.persistence.criteria.Predicate isOwnerPredicate = cb.equal(root.get("owner").get("id"), currentUserId);
                    predicates.add(cb.or(isPublicPredicate, isOwnerPredicate));
                } else {
                    predicates.add(isPublicPredicate);
                }
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
                predicates.add(cb.equal(root.get("owner").get("id"), user.getId()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Event> events = eventRepository.findAll(spec, pageable);

        // Apply mine=true filter (owned-by-current-user) as a post-filter.
        // This avoids exposing arbitrary ownerId querying.
        if (Boolean.TRUE.equals(request.getMine()) && user != null) {
            List<Event> owned = events.getContent().stream()
                    .filter(e -> e.getOwner() != null && e.getOwner().getId().equals(user.getId()))
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
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));

        if (Boolean.TRUE.equals(event.getIsArchived())) {
            throw new ConflictException("Event is already archived");
        }

        event.setIsArchived(true);
        event.setArchivedAt(LocalDateTime.now());
        if (archivedBy != null) {
            UserAccount archivedByUser = userAccountRepository.findById(archivedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + archivedBy));
            event.setArchivedBy(archivedByUser);
        } else {
            event.setArchivedBy(null);
        }
        event.setArchiveReason(reason);

        // Cancel all pending tickets when event is archived
        if (ticketRepository != null) {
            try {
                List<Ticket> pendingTickets = ticketRepository.findByEventIdAndStatus(id, TicketStatus.PENDING);
                if (!pendingTickets.isEmpty()) {
                    for (Ticket ticket : pendingTickets) {
                        ticket.cancel("Event archived: " + (reason != null ? reason : "Event no longer available"));
                    }
                    ticketRepository.saveAll(pendingTickets);
                    log.info("Cancelled {} pending tickets for archived event {}", pendingTickets.size(), id);
                }

                // Notify holders of ISSUED tickets
                List<Ticket> issuedTickets = ticketRepository.findByEventIdAndStatus(id, TicketStatus.ISSUED);
                if (!issuedTickets.isEmpty()) {
                    sendEventArchivedNotifications(event, issuedTickets);
                }
            } catch (Exception e) {
                log.warn("Failed to cancel pending tickets for archived event {}: {}", id, e.getMessage());
            }
        }

        // Update attendee statuses to DECLINED for archived events
        if (attendeeRepository != null) {
            try {
                List<eventplanner.features.attendee.entity.Attendee> attendees = 
                    attendeeRepository.findByEventId(id);
                int updatedCount = 0;
                for (eventplanner.features.attendee.entity.Attendee attendee : attendees) {
                    if (attendee.getRsvpStatus() == eventplanner.features.attendee.enums.AttendeeStatus.CONFIRMED ||
                        attendee.getRsvpStatus() == eventplanner.features.attendee.enums.AttendeeStatus.PENDING) {
                        attendee.setRsvpStatus(eventplanner.features.attendee.enums.AttendeeStatus.DECLINED);
                        updatedCount++;
                    }
                }
                if (updatedCount > 0) {
                    attendeeRepository.saveAll(attendees);
                    log.info("Updated {} attendee statuses to DECLINED for archived event {}", updatedCount, id);
                    
                    // Recalculate attendee count after status updates
                    recalculateAttendeeCount(id);
                }
            } catch (Exception e) {
                log.warn("Failed to update attendee statuses for archived event {}: {}", id, e.getMessage());
            }
        }

        return eventRepository.save(event);
    }

    /**
     * Send notifications to ticket holders when event is archived.
     * Uses sendOrThrow to ensure ticket holders are notified of critical event cancellation.
     */
    private void sendEventArchivedNotifications(Event event, List<Ticket> tickets) {
        if (notificationService == null || externalServicesProperties == null) {
            log.warn("Notification service not available, skipping event archived notifications");
            return;
        }

        for (Ticket ticket : tickets) {
            String recipient = ticket.getOwnerEmail() != null
                ? ticket.getOwnerEmail()
                : (ticket.getAttendee() != null ? ticket.getAttendee().getEmail() : null);

            if (recipient != null) {
                Map<String, Object> vars = new HashMap<>();
                vars.put("eventName", event.getName());
                vars.put("ticketNumber", ticket.getTicketNumber());
                if (event.getArchiveReason() != null) {
                    vars.put("reason", event.getArchiveReason());
                }

                // CRITICAL: Ticket holders MUST be notified of event cancellation
                // If notification fails, archive operation should fail (transaction rollback)
                notificationService.sendOrThrow(NotificationRequest.builder()
                    .type(CommunicationType.EMAIL)
                    .to(recipient)
                    .subject("Event Cancelled: " + event.getName())
                    .templateId("event-archived")
                    .templateVariables(vars)
                    .eventId(event.getId())
                    .from(externalServicesProperties.getEmail().getFromEvents())
                    .build());
            }
        }
    }

    /**
     * Restore an archived event
     * @param id The event ID
     * @param restoredBy The user ID restoring the event
     * @return Restored event
     */
    public Event restoreEvent(UUID id, UUID restoredBy) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));

        if (!Boolean.TRUE.equals(event.getIsArchived())) {
            throw new ConflictException("Event is not archived");
        }

        event.setIsArchived(false);
        event.setRestoredAt(LocalDateTime.now());
        if (restoredBy != null) {
            UserAccount restoredByUser = userAccountRepository.findById(restoredBy)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + restoredBy));
            event.setRestoredBy(restoredByUser);
        } else {
            event.setRestoredBy(null);
        }
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
     * Convert Event entity to EventResponse with user-specific context.
     * 
     * @param event The event entity
     * @param user The authenticated user (null for anonymous - userContext will be null)
     * @return EventResponse with user context populated if user is provided
     */
    public EventResponse toResponse(Event event, UserPrincipal user) {
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
        response.setCoverImageUrl(event.getCoverImageUrl());
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
        response.setOwnerId(event.getOwner() != null ? event.getOwner().getId() : null);
        response.setVenueId(event.getVenueId());
        response.setVenue(event.getVenue() != null ? toVenueDTO(event.getVenue()) : null);
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        response.setScope(EventScope.FULL); // Full details response
        
        // Access control settings
        response.setAccessType(event.getAccessType() != null ? event.getAccessType() : EventAccessType.OPEN);
        response.setFeedsPublicAfterEvent(event.getFeedsPublicAfterEvent());
        
        // Compute and set user-specific context
        if (userEventContextService != null) {
            UserEventContext userContext = userEventContextService.buildContext(event, user);
            response.setUserContext(userContext);
        }
        
        // Populate ticket types information for ticketed events
        populateTicketInfo(event, response);
        
        return response;
    }

    /**
     * Populate ticket information for an event response.
     * Only populates for TICKETED events.
     */
    private void populateTicketInfo(Event event, EventResponse response) {
        if (event.getAccessType() != EventAccessType.TICKETED) {
            return;
        }
        
        if (ticketTypeRepository == null) {
            return;
        }
        
        List<TicketType> ticketTypes = ticketTypeRepository.findByEventIdAndIsActiveTrue(event.getId());
        if (ticketTypes == null || ticketTypes.isEmpty()) {
            response.setTicketTypes(List.of());
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        List<TicketTypeSummary> summaries = ticketTypes.stream()
                .map(tt -> toTicketTypeSummary(tt, now))
                .collect(Collectors.toList());
        
        response.setTicketTypes(summaries);
    }

    /**
     * Convert TicketType entity to TicketTypeSummary DTO.
     */
    private TicketTypeSummary toTicketTypeSummary(TicketType tt, LocalDateTime now) {
        int available = tt.getQuantityAvailable() != null ? tt.getQuantityAvailable() : 0;
        int sold = tt.getQuantitySold() != null ? tt.getQuantitySold() : 0;
        int reserved = tt.getQuantityReserved() != null ? tt.getQuantityReserved() : 0;

        int remaining = available - sold - reserved;
        remaining = Math.max(0, remaining);
        
        boolean isFree = tt.getPriceMinor() == null || tt.getPriceMinor() == 0;
        
        // Determine if currently on sale
        boolean isOnSale = true;
        if (tt.getSaleStartDate() != null && now.isBefore(tt.getSaleStartDate())) {
            isOnSale = false;
        }
        if (tt.getSaleEndDate() != null && now.isAfter(tt.getSaleEndDate())) {
            isOnSale = false;
        }
        
        boolean isActive = Boolean.TRUE.equals(tt.getIsActive());
        boolean isAvailable = isActive && isOnSale && remaining > 0;
        
        // Generate status message
        String statusMessage = generateTicketStatusMessage(tt, remaining, isOnSale, now);
        
        return TicketTypeSummary.builder()
                .id(tt.getId())
                .name(tt.getName())
                .category(tt.getCategory())
                .description(tt.getDescription())
                .priceMinor(tt.getPriceMinor())
                .currency(tt.getCurrency())
                .isFree(isFree)
                .quantityTotal(available)
                .quantityRemaining(remaining)
                .quantitySold(sold)
                .isAvailable(isAvailable)
                .isActive(isActive)
                .saleStartDate(tt.getSaleStartDate())
                .saleEndDate(tt.getSaleEndDate())
                .isOnSale(isOnSale)
                .maxPerPerson(tt.getMaxTicketsPerPerson())
                .requiresApproval(tt.getRequiresApproval())
                .statusMessage(statusMessage)
                .build();
    }

    /**
     * Generate a user-friendly status message for a ticket type.
     */
    private String generateTicketStatusMessage(TicketType tt, int remaining, boolean isOnSale, LocalDateTime now) {
        if (!Boolean.TRUE.equals(tt.getIsActive())) {
            return "Not available";
        }
        
        if (tt.getSaleStartDate() != null && now.isBefore(tt.getSaleStartDate())) {
            return "Sales start soon";
        }
        
        if (tt.getSaleEndDate() != null && now.isAfter(tt.getSaleEndDate())) {
            return "Sales ended";
        }
        
        if (remaining <= 0) {
            return "Sold out";
        }
        
        if (remaining <= 5) {
            return "Only " + remaining + " left!";
        }
        
        if (remaining <= 10) {
            return "Limited availability";
        }
        
        return "Available";
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


    // ==================== EVENT CAPACITY & REGISTRATION METHODS ====================

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

    // ==================== EVENT VISIBILITY & ACCESS CONTROL METHODS ====================

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
        venue.syncLocation();
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
        
        // Use database-level pagination to avoid loading all posts into memory
        org.springframework.data.domain.Page<FeedPost> postPage = aggregateFeedPostsPaginated(event, user, request, page, size);
        
        // Set posts and pagination metadata
        feed.setPosts(postPage.getContent());
        feed.setCurrentPage(page);
        feed.setPageSize(size);
        feed.setTotalPosts(postPage.getTotalElements());
        feed.setTotalPages(postPage.getTotalPages());
        feed.setHasNext(postPage.hasNext());
        feed.setHasPrevious(postPage.hasPrevious());
        feed.setScope(EventScope.FEED); // Feed view response
        
        // Access control settings
        feed.setAccessType(event.getAccessType() != null ? event.getAccessType() : EventAccessType.OPEN);
        feed.setFeedsPublicAfterEvent(event.getFeedsPublicAfterEvent());
        
        // Compute and set user-specific context
        if (userEventContextService != null) {
            UserEventContext userContext = userEventContextService.buildContext(event, user);
            feed.setUserContext(userContext);
        }
        
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
     * Aggregate feed posts with database-level pagination to prevent OOM for popular events.
     */
    private org.springframework.data.domain.Page<FeedPost> aggregateFeedPostsPaginated(
            Event event, UserPrincipal user, EventFeedRequest request, int page, int size) {
        
        // Don't show feed posts for archived events
        if (Boolean.TRUE.equals(event.getIsArchived())) {
            return new org.springframework.data.domain.PageImpl<>(new ArrayList<>());
        }

        // Validate access based on event access type
        if (!hasEventFeedAccess(event, user)) {
            return new org.springframework.data.domain.PageImpl<>(new ArrayList<>());
        }

        // Use database-level pagination
        if (eventPostRepository == null) {
            return new org.springframework.data.domain.PageImpl<>(new ArrayList<>());
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
            page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<EventFeedPost> eventPostPage = 
            eventPostRepository.findByEventIdOrderByCreatedAtDesc(event.getId(), pageable);

        // Fetch authors in one shot
        Map<java.util.UUID, UserAccount> authorsById = Map.of();
        if (userAccountRepository != null && !eventPostPage.getContent().isEmpty()) {
            Set<java.util.UUID> authorIds = eventPostPage.getContent().stream()
                    .map(EventFeedPost::getCreatedBy)
                    .filter(java.util.Objects::nonNull)
                    .map(UserAccount::getId)
                    .collect(Collectors.toSet());
            if (!authorIds.isEmpty()) {
                authorsById = userAccountRepository.findAllById(authorIds).stream()
                        .collect(Collectors.toMap(UserAccount::getId, a -> a));
            }
        }

        // Batch load media objects to avoid N+1 queries
        Map<java.util.UUID, EventStoredObject> mediaObjectsById = Map.of();
        if (eventStoredObjectRepository != null) {
            Set<java.util.UUID> mediaObjectIds = eventPostPage.getContent().stream()
                    .map(EventFeedPost::getMediaObjectId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!mediaObjectIds.isEmpty()) {
                mediaObjectsById = eventStoredObjectRepository.findAllById(mediaObjectIds).stream()
                        .collect(Collectors.toMap(EventStoredObject::getId, o -> o));
            }
        }

        final Map<java.util.UUID, UserAccount> finalAuthorsById = authorsById;
        final Map<java.util.UUID, EventStoredObject> finalMediaById = mediaObjectsById;
        
        List<FeedPost> feedPosts = eventPostPage.getContent().stream().map(p -> {
            FeedPost fp = new FeedPost();
            fp.setId(p.getId());
            fp.setType(p.getPostType() != null ? p.getPostType().name() : "TEXT");
            fp.setContent(p.getContent());
            fp.setPostedAt(p.getCreatedAt());

            UserAccount author = p.getCreatedBy() != null ? finalAuthorsById.get(p.getCreatedBy().getId()) : null;
            fp.setAuthorName(author != null ? author.getName() : null);
            fp.setAuthorAvatarUrl(author != null ? author.getProfilePictureUrl() : null);

            // Attach media if available (using batch-loaded objects)
            if (p.getMediaObjectId() != null && s3StorageService != null) {
                EventStoredObject obj = finalMediaById.get(p.getMediaObjectId());
                if (obj != null && obj.getEvent() != null && event.getId().equals(obj.getEvent().getId())) {
                    fp.setMediaUrl(s3StorageService.generatePresignedGetUrl(BucketAlias.EVENT, obj.getObjectKey(), java.time.Duration.ofMinutes(10)).toString());
                }
            }

            fp.setLikes(0);
            fp.setComments(0);
            return fp;
        }).collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(feedPosts, pageable, eventPostPage.getTotalElements());
    }

    /**
     * Check if a user has access to view event feed posts based on event access type.
     */
    private boolean hasEventFeedAccess(Event event, UserPrincipal user) {
        EventAccessType accessType = event.getAccessType() != null ? event.getAccessType() : EventAccessType.OPEN;
        
        if (accessType == EventAccessType.TICKETED && user != null && ticketRepository != null) {
            boolean hasValidTicket = ticketRepository.hasValidTicketByUserId(event.getId(), user.getId());
            if (!hasValidTicket && user.getUsername() != null) {
                hasValidTicket = ticketRepository.hasValidTicketByEmail(event.getId(), user.getUsername());
            }
            if (!hasValidTicket && 
                (event.getOwner() == null || !event.getOwner().getId().equals(user.getId()))) {
                boolean isCollaborator = authorizationService != null && 
                    authorizationService.hasEventMembership(user, event.getId());
                if (!isCollaborator) {
                    return false;
                }
            }
        }
        
        if (accessType == EventAccessType.RSVP_REQUIRED && user != null && attendeeRepository != null) {
            Optional<eventplanner.features.attendee.entity.Attendee> attendee = 
                attendeeRepository.findByEventIdAndUserId(event.getId(), user.getId());
            if (attendee.isEmpty() || 
                attendee.get().getRsvpStatus() != eventplanner.features.attendee.enums.AttendeeStatus.CONFIRMED) {
                if (event.getOwner() == null || !event.getOwner().getId().equals(user.getId())) {
                    boolean isCollaborator = authorizationService != null && 
                        authorizationService.hasEventMembership(user, event.getId());
                    if (!isCollaborator) {
                        return false;
                    }
                }
            }
        }
        
        return true;
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
            throw new ResourceNotFoundException("Event not found or access denied");
        }

        return toFeedResponse(eventOpt.get(), user, request);
    }

    /**
     * Get personalized "For You" feed based on user interests and preferences
     * @param request List request with pagination
     * @param user The user principal
     * @return Page of recommended events
     */
    public Page<Event> getForYouFeed(EventListRequest request, UserPrincipal user) {
        if (user == null) {
            throw new UnauthorizedException("Authentication required for For You feed");
        }
        
        // For now, return public events sorted by relevance (can be enhanced with ML/recommendations)
        // Priority: events from users you've interacted with, events in your location, upcoming events
        EventListRequest forYouRequest = new EventListRequest();
        if (request != null) {
            forYouRequest.setPage(request.getPage() != null ? request.getPage() : 0);
            forYouRequest.setSize(request.getSize() != null ? request.getSize() : 20);
        } else {
            forYouRequest.setPage(0);
            forYouRequest.setSize(20);
        }
        forYouRequest.setIsPublic(true);
        forYouRequest.setTimeframe("UPCOMING");
        forYouRequest.setSortBy("startDateTime");
        forYouRequest.setSortDirection("ASC");
        
        Page<Event> events = listEvents(forYouRequest, user);
        
        // TODO: Enhance with personalized recommendations based on:
        // - Past event attendance
        // - User interests/preferences
        // - Similar events to ones user has attended
        // - Events from users/organizations user follows
        
        return events;
    }

    /**
     * Get "Following" feed - events from users/organizations you follow
     * @param request List request with pagination
     * @param user The user principal
     * @return Page of events from followed users
     */
    public Page<Event> getFollowingFeed(EventListRequest request, UserPrincipal user) {
        if (user == null) {
            throw new UnauthorizedException("Authentication required for Following feed");
        }

        if (userFollowRepository == null) {
            log.warn("UserFollowRepository not available, returning empty following feed");
            return Page.empty();
        }

        // Get list of user IDs that the current user follows
        List<UUID> followedUserIds = userFollowRepository.findFollowedUserIdsByFollowerId(user.getId());

        if (followedUserIds.isEmpty()) {
            // User doesn't follow anyone, return empty page
            log.debug("User {} doesn't follow anyone yet", user.getId());
            return Page.empty();
        }

        // Build pageable
        int pageNum = request != null && request.getPage() != null ? request.getPage() : 0;
        int pageSize = request != null && request.getSize() != null ? request.getSize() : 20;
        pageSize = Math.min(100, Math.max(1, pageSize)); // Enforce limits

        Sort sort = Sort.by(Sort.Direction.DESC, "startDateTime"); // Most recent events first
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

        // Query events owned by followed users
        // Only show public events or events the user has access to
        Page<Event> events = eventRepository.findByOwnerIdInAndIsPublicTrue(followedUserIds, pageable);

        log.debug("User {} following feed: {} events from {} followed users",
                user.getId(), events.getTotalElements(), followedUserIds.size());

        return events;
    }

    // ==================== ATTENDEE COUNT SYNCHRONIZATION ====================

    /**
     * Recalculate event attendee count from actual confirmed attendees.
     * This is the single source of truth for attendee count accuracy.
     * Use this method to fix discrepancies or after bulk operations.
     * 
     * @param eventId The event ID
     * @return The recalculated count
     */
    @Transactional
    public int recalculateAttendeeCount(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
        
        int actualCount = 0;
        
        if (attendeeRepository != null) {
            // Count confirmed attendees from the database
            List<eventplanner.features.attendee.entity.Attendee> attendees = 
                attendeeRepository.findByEventId(eventId);
            
            // Count unique confirmed attendees (by user ID or email)
            java.util.Set<UUID> processedUserIds = new java.util.HashSet<>();
            java.util.Set<String> processedEmails = new java.util.HashSet<>();
            
            for (eventplanner.features.attendee.entity.Attendee attendee : attendees) {
                if (attendee.getRsvpStatus() == eventplanner.features.attendee.enums.AttendeeStatus.CONFIRMED) {
                    UUID userId = attendee.getUser() != null ? attendee.getUser().getId() : null;
                    String email = attendee.getEmail();
                    
                    // Count once per unique user or email
                    if (userId != null && !processedUserIds.contains(userId)) {
                        processedUserIds.add(userId);
                        actualCount++;
                    } else if (email != null && !processedEmails.contains(email.toLowerCase())) {
                        processedEmails.add(email.toLowerCase());
                        actualCount++;
                    }
                }
            }
        }
        
        // Update event with recalculated count
        event.setCurrentAttendeeCount(Math.max(0, actualCount));
        eventRepository.save(event);
        
        log.info("Recalculated attendee count for event {}: {} (was {})", 
                eventId, actualCount, event.getCurrentAttendeeCount());
        
        return actualCount;
    }

    /**
     * Validate access type change to prevent invalid transitions.
     */
    private void validateAccessTypeChange(Event event, EventAccessType oldType, EventAccessType newType) {
        // Prevent changing from TICKETED to non-TICKETED if tickets exist
        if (oldType == EventAccessType.TICKETED && newType != EventAccessType.TICKETED) {
            if (ticketRepository != null) {
                // Count all tickets (any status) for the event
                List<Ticket> allTickets = ticketRepository.findByEventIdAndStatusIn(
                    event.getId(), 
                    java.util.Arrays.asList(
                        TicketStatus.PENDING, 
                        TicketStatus.ISSUED, 
                        TicketStatus.VALIDATED,
                        TicketStatus.CANCELLED,
                        TicketStatus.REFUNDED
                    )
                );
                if (!allTickets.isEmpty()) {
                    throw new BadRequestException(
                        "Cannot change access type from TICKETED when tickets exist. " +
                        "Please cancel or refund all tickets first.");
                }
            }
        }
        
        // Warn about restricting access (log only, don't prevent)
        if (isAccessBeingRestricted(oldType, newType)) {
            log.warn("Event {} access type changed from {} to {} - this may restrict feed access for some users",
                    event.getId(), oldType, newType);
        }
    }

    /**
     * Check if access type change is restricting access.
     */
    private boolean isAccessBeingRestricted(EventAccessType oldType, EventAccessType newType) {
        // OPEN -> anything else is restricting
        if (oldType == EventAccessType.OPEN) {
            return newType != EventAccessType.OPEN;
        }
        // RSVP_REQUIRED -> TICKETED or INVITE_ONLY is more restrictive
        if (oldType == EventAccessType.RSVP_REQUIRED) {
            return newType == EventAccessType.TICKETED || newType == EventAccessType.INVITE_ONLY;
        }
        return false;
    }

}
