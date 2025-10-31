package ai.eventplanner.event.service;

import ai.eventplanner.auth.service.UserPrincipal;
import ai.eventplanner.common.domain.enums.EventUserType;
import ai.eventplanner.event.entity.Event;
import ai.eventplanner.event.repo.EventRepository;
import ai.eventplanner.user.entity.EventUser;
import ai.eventplanner.user.repo.EventUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public EventAccessControlService(EventRepository eventRepository,
                                     EventUserRepository eventUserRepository) {
        this.eventRepository = eventRepository;
        this.eventUserRepository = eventUserRepository;
    }

    public Event ensureEventExists(UUID eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    public Event requireMediaView(UserPrincipal principal, UUID eventId) {
        Event event = ensureEventExists(eventId);
        if (Boolean.TRUE.equals(event.getIsPublic())) {
            return event;
        }
        requireAuthenticated(principal);
        if (isOwner(principal, event)) {
            return event;
        }
        EventUser membership = getMembership(eventId, principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event media"));
        if (!MEDIA_VIEW_ROLES.contains(membership.getUserType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event media");
        }
        return event;
    }

    public Event requireMediaUpload(UserPrincipal principal, UUID eventId) {
        Event event = ensureEventExists(eventId);
        requireAuthenticated(principal);
        if (isOwner(principal, event)) {
            return event;
        }
        // Any event membership is allowed to upload media
        getMembership(eventId, principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to upload media"));
        return event;
    }

    public Event requireMediaManage(UserPrincipal principal, UUID eventId) {
        Event event = ensureEventExists(eventId);
        requireAuthenticated(principal);
        if (isOwner(principal, event)) {
            return event;
        }
        EventUser membership = getMembership(eventId, principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to manage media"));
        if (!MEDIA_MANAGE_ROLES.contains(membership.getUserType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to manage media");
        }
        return event;
    }

    public Event requireAssetView(UserPrincipal principal, UUID eventId) {
        Event event = ensureEventExists(eventId);
        requireAuthenticated(principal);
        if (isOwner(principal, event)) {
            return event;
        }
        EventUser membership = getMembership(eventId, principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event assets"));
        if (!ASSET_VIEW_ROLES.contains(membership.getUserType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event assets");
        }
        return event;
    }

    public Event requireCoverManage(UserPrincipal principal, UUID eventId) {
        return requireMediaManage(principal, eventId);
    }

    private void requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private boolean isOwner(UserPrincipal principal, Event event) {
        return principal != null && event.getOwnerId() != null && event.getOwnerId().equals(principal.getId());
    }

    private Optional<EventUser> getMembership(UUID eventId, UUID userId) {
        return eventUserRepository.findByEventIdAndUserId(eventId, userId);
    }
}
