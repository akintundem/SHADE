package eventplanner.features.collaboration.service;

import eventplanner.common.domain.enums.RegistrationStatus;
import eventplanner.features.collaboration.dto.request.EventCollaboratorRequest;
import eventplanner.features.collaboration.dto.response.EventCollaboratorResponse;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EventCollaboratorService {

    private final EventUserRepository eventUserRepository;
    private final EventRepository eventRepository;
    private final UserAccountRepository userAccountRepository;

    public EventCollaboratorService(EventUserRepository eventUserRepository, 
                                   EventRepository eventRepository,
                                   UserAccountRepository userAccountRepository) {
        this.eventUserRepository = eventUserRepository;
        this.eventRepository = eventRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public List<EventCollaboratorResponse> getCollaborators(UUID eventId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return eventUserRepository.findByEventId(eventId, PageRequest.of(safePage, safeSize)).getContent().stream()
                .map(eventUser -> toResponse(eventUser, Collections.emptyList(), false, null))
                .toList();
    }

    public EventCollaboratorResponse addCollaborator(UUID eventId, EventCollaboratorRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("role is required");
        }

        UserAccount user = userAccountRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (eventUserRepository.findByEventIdAndUserId(eventId, user.getId()).isPresent()) {
            throw new IllegalArgumentException("User is already a collaborator on this event");
        }

        // Fetch Event entity
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        
        EventUser collaborator = new EventUser();
        collaborator.setEvent(event);
        collaborator.setUser(user);
        collaborator.setUserType(request.getRole());
        collaborator.setRegistrationStatus(RegistrationStatus.CONFIRMED);
        collaborator.setRegistrationDate(LocalDateTime.now());

        EventUser saved = eventUserRepository.save(collaborator);
        return toResponse(saved, request.getPermissions(), false, null);
    }

    public EventCollaboratorResponse updateCollaborator(UUID eventId, UUID collaboratorId, EventCollaboratorRequest request) {
        EventUser collaborator = eventUserRepository.findById(collaboratorId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found"));

        if (collaborator.getEvent() == null || !collaborator.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Collaborator does not belong to event");
        }

        if (request != null && request.getUserId() != null) {
            UserAccount user = userAccountRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            eventUserRepository.findByEventIdAndUserId(eventId, user.getId())
                    .filter(existing -> !existing.getId().equals(collaboratorId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("User is already a collaborator on this event");
                    });
            collaborator.setUser(user);
        }
        if (request != null && request.getRole() != null) {
            collaborator.setUserType(request.getRole());
        }

        EventUser saved = eventUserRepository.save(collaborator);
        return toResponse(saved, request != null ? request.getPermissions() : null, false, null);
    }

    public void removeCollaborator(UUID eventId, UUID collaboratorId) {
        EventUser collaborator = eventUserRepository.findById(collaboratorId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found"));
        if (collaborator.getEvent() == null || !collaborator.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Collaborator does not belong to event");
        }
        eventUserRepository.delete(collaborator);
    }

    private EventCollaboratorResponse toResponse(EventUser eventUser,
                                                 List<String> permissions,
                                                 boolean invitationSent,
                                                 LocalDateTime invitationSentAt) {
        EventCollaboratorResponse response = new EventCollaboratorResponse();
        response.setCollaboratorId(eventUser.getId());
        response.setEventId(eventUser.getEvent() != null ? eventUser.getEvent().getId() : null);
        response.setUserId(eventUser.getUser() != null ? eventUser.getUser().getId() : null);
        response.setEmail(eventUser.getUser() != null ? eventUser.getUser().getEmail() : null);
        response.setUserName(eventUser.getUser() != null ? eventUser.getUser().getName() : null);
        response.setRole(eventUser.getUserType());
        response.setPermissions(permissions != null ? permissions : Collections.emptyList());
        response.setRegistrationStatus(eventUser.getRegistrationStatus() != null ? eventUser.getRegistrationStatus().name() : null);
        response.setInvitationSent(invitationSent);
        response.setInvitationSentAt(invitationSentAt);
        response.setAddedAt(eventUser.getCreatedAt());
        response.setUpdatedAt(eventUser.getUpdatedAt());
        return response;
    }
}





