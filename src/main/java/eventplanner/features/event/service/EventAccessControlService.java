package eventplanner.features.event.service;

import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.domain.enums.EventAccessType;
import eventplanner.common.domain.enums.EventStatus;
import eventplanner.common.domain.enums.EventUserType;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.ticket.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class EventAccessControlService {

    private static final Set<EventUserType> MEDIA_VIEW_ROLES = EnumSet.of(EventUserType.ORGANIZER, EventUserType.COORDINATOR, EventUserType.ADMIN, EventUserType.COLLABORATOR);
    private static final Set<EventUserType> ASSET_VIEW_ROLES = EnumSet.of(EventUserType.ORGANIZER, EventUserType.COORDINATOR, EventUserType.ADMIN);
    private static final Set<EventUserType> MEDIA_MANAGE_ROLES = EnumSet.of(EventUserType.ORGANIZER, EventUserType.COORDINATOR, EventUserType.ADMIN);

    private final EventRepository eventRepository;
    private final EventUserRepository eventUserRepository;
    private final TicketRepository ticketRepository;
    private final AttendeeRepository attendeeRepository;

    public EventAccessControlService(EventRepository eventRepository,
                                     EventUserRepository eventUserRepository,
                                     TicketRepository ticketRepository,
                                     AttendeeRepository attendeeRepository) {
        this.eventRepository = eventRepository;
        this.eventUserRepository = eventUserRepository;
        this.ticketRepository = ticketRepository;
        this.attendeeRepository = attendeeRepository;
    }

    public Event ensureEventExists(UUID eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    public Event requireMediaView(UserPrincipal principal, UUID eventId) {
        Event event = ensureEventExists(eventId);
        
        // Get access type (default to OPEN)
        EventAccessType accessType = event.getAccessType() != null ? event.getAccessType() : EventAccessType.OPEN;
        
        // Handle access based on access type
        switch (accessType) {
            case OPEN:
                return requireOpenEventMediaView(principal, event);
            case RSVP_REQUIRED:
                return requireRsvpRequiredMediaView(principal, event);
            case INVITE_ONLY:
                return requireInviteOnlyMediaView(principal, event);
            case TICKETED:
                return requireTicketedEventMediaView(principal, event);
            default:
                return requireOpenEventMediaView(principal, event);
        }
    }

    /**
     * Handle media view access for OPEN events.
     * Public events are accessible to all, private events require membership.
     */
    private Event requireOpenEventMediaView(UserPrincipal principal, Event event) {
        // Public events are accessible to all
        if (Boolean.TRUE.equals(event.getIsPublic())) {
            return event;
        }
        
        // Private open events require authentication and membership
        UserPrincipal user = requireAuthenticated(principal);
        if (isOwner(user, event)) {
            return event;
        }
        EventUser membership = getMembership(event.getId(), user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event media"));
        if (!MEDIA_VIEW_ROLES.contains(membership.getUserType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event media");
        }
        return event;
    }

    /**
     * Handle media view access for RSVP_REQUIRED events.
     * Requires confirmed RSVP unless event is completed with public feeds.
     */
    private Event requireRsvpRequiredMediaView(UserPrincipal principal, Event event) {
        // Owner always has access
        if (principal != null && isOwner(principal, event)) {
            return event;
        }
        
        // Check if event is completed and feeds are public after event
        if (isEventCompleted(event) && Boolean.TRUE.equals(event.getFeedsPublicAfterEvent())) {
            return event;
        }
        
        UserPrincipal user = requireAuthenticated(principal);
        UUID userId = user.getId();
        
        // Check if user is an organizer/coordinator
        Optional<EventUser> membership = getMembership(event.getId(), userId);
        if (membership.isPresent() && MEDIA_VIEW_ROLES.contains(membership.get().getUserType())) {
            return event;
        }
        
        // Check if user has confirmed RSVP
        if (hasConfirmedRsvp(event.getId(), userId)) {
            return event;
        }
        
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
            "Access denied. RSVP confirmation is required to view this event's content.");
    }

    /**
     * Handle media view access for INVITE_ONLY events.
     * Requires accepted invite unless event is completed with public feeds.
     */
    private Event requireInviteOnlyMediaView(UserPrincipal principal, Event event) {
        // Owner always has access
        if (principal != null && isOwner(principal, event)) {
            return event;
        }
        
        // Check if event is completed and feeds are public after event
        if (isEventCompleted(event) && Boolean.TRUE.equals(event.getFeedsPublicAfterEvent())) {
            return event;
        }
        
        UserPrincipal user = requireAuthenticated(principal);
        UUID userId = user.getId();
        
        // Check if user is an organizer/coordinator
        Optional<EventUser> membership = getMembership(event.getId(), userId);
        if (membership.isPresent() && MEDIA_VIEW_ROLES.contains(membership.get().getUserType())) {
            return event;
        }
        
        // Check if user has accepted invite (confirmed RSVP for invite-only = accepted invite)
        if (hasConfirmedRsvp(event.getId(), userId)) {
            return event;
        }
        
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
            "Access denied. You must be invited and accept the invitation to view this event's content.");
    }

    /**
     * Handle media view access for TICKETED events.
     * Requires valid ticket unless event is completed with public feeds.
     */
    private Event requireTicketedEventMediaView(UserPrincipal principal, Event event) {
        // Owner always has access
        if (principal != null && isOwner(principal, event)) {
            return event;
        }
        
        // Check if event is completed and feeds are public after event
        if (isEventCompleted(event) && Boolean.TRUE.equals(event.getFeedsPublicAfterEvent())) {
            return event;
        }
        
        UserPrincipal user = requireAuthenticated(principal);
        UUID userId = user.getId();
        
        // Check if user is an organizer/coordinator
        Optional<EventUser> membership = getMembership(event.getId(), userId);
        if (membership.isPresent() && MEDIA_VIEW_ROLES.contains(membership.get().getUserType())) {
            return event;
        }
        
        // Check if user has a valid ticket
        if (hasValidTicket(event.getId(), user)) {
            return event;
        }
        
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
            "Access denied. A valid ticket is required to view this event's content.");
    }

    /**
     * Check if user has a confirmed RSVP for the event.
     */
    private boolean hasConfirmedRsvp(UUID eventId, UUID userId) {
        Optional<Attendee> attendee = attendeeRepository.findByEventIdAndUserId(eventId, userId);
        return attendee.isPresent() && attendee.get().getRsvpStatus() == AttendeeStatus.CONFIRMED;
    }

    /**
     * Check if an event is completed (past end date or COMPLETED status).
     */
    private boolean isEventCompleted(Event event) {
        // Check status first
        if (event.getEventStatus() == EventStatus.COMPLETED) {
            return true;
        }
        
        // Check if event end date has passed
        if (event.getEndDateTime() != null) {
            return LocalDateTime.now().isAfter(event.getEndDateTime());
        }
        
        // If no end date, check start date (assume single-day event)
        if (event.getStartDateTime() != null) {
            return LocalDateTime.now().isAfter(event.getStartDateTime().plusDays(1));
        }
        
        return false;
    }

    /**
     * Check if user has a valid ticket for the event.
     * Checks both via user ID (attendee relationship) and email.
     */
    private boolean hasValidTicket(UUID eventId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            return false;
        }
        
        // Check by user ID (via attendee relationship)
        if (ticketRepository.hasValidTicketByUserId(eventId, principal.getId())) {
            return true;
        }
        
        // Check by email (for email-only tickets) - getUsername() returns email
        String email = principal.getUsername();
        if (email != null && !email.isEmpty()) {
            return ticketRepository.hasValidTicketByEmail(eventId, email);
        }
        
        return false;
    }

    public Event requireMediaUpload(UserPrincipal principal, UUID eventId) {
        Event event = ensureEventExists(eventId);
        UserPrincipal user = requireAuthenticated(principal);
        if (isOwner(user, event)) {
            return event;
        }
        // Any event membership is allowed to upload media
        getMembership(eventId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to upload media"));
        return event;
    }

    public Event requireMediaManage(UserPrincipal principal, UUID eventId) {
        Event event = ensureEventExists(eventId);
        UserPrincipal user = requireAuthenticated(principal);
        if (isOwner(user, event)) {
            return event;
        }
        EventUser membership = getMembership(eventId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to manage media"));
        if (!MEDIA_MANAGE_ROLES.contains(membership.getUserType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to manage media");
        }
        return event;
    }

    public Event requireAssetView(UserPrincipal principal, UUID eventId) {
        Event event = ensureEventExists(eventId);
        UserPrincipal user = requireAuthenticated(principal);
        if (isOwner(user, event)) {
            return event;
        }
        EventUser membership = getMembership(eventId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event assets"));
        if (!ASSET_VIEW_ROLES.contains(membership.getUserType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event assets");
        }
        return event;
    }

    public Event requireCoverManage(UserPrincipal principal, UUID eventId) {
        return requireMediaManage(principal, eventId);
    }

    private UserPrincipal requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }

    private boolean isOwner(UserPrincipal principal, Event event) {
        return principal != null && event.getOwner() != null && event.getOwner().getId().equals(principal.getId());
    }

    private Optional<EventUser> getMembership(UUID eventId, UUID userId) {
        return eventUserRepository.findByEventIdAndUserId(eventId, userId);
    }
}
