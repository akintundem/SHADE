package eventplanner.features.event.service;

import eventplanner.common.domain.enums.RegistrationStatus;
import eventplanner.features.event.dto.request.EventCollaboratorRequest;
import eventplanner.features.event.dto.response.EventCollaboratorResponse;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.repository.EventUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;

@Service
@Transactional
public class EventCollaboratorService {

    private final EventUserRepository eventUserRepository;

    public EventCollaboratorService(EventUserRepository eventUserRepository) {
        this.eventUserRepository = eventUserRepository;
    }

    public List<EventCollaboratorResponse> getCollaborators(UUID eventId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return eventUserRepository.findByEventId(eventId, PageRequest.of(safePage, safeSize)).getContent().stream()
            .map(eventUser -> toResponse(eventUser, Collections.emptyList(), false, null))
            .toList();
    }

    public EventCollaboratorResponse addCollaborator(UUID eventId, EventCollaboratorRequest request) {
        EventUser collaborator = new EventUser();
        collaborator.setEventId(eventId);
        collaborator.setUserId(request.getUserId());
        collaborator.setEmail(request.getEmail());
        collaborator.setUserType(request.getRole());
        collaborator.setRegistrationStatus(RegistrationStatus.CONFIRMED);
        collaborator.setRegistrationDate(LocalDateTime.now());
        collaborator.setNotes(request.getNotes());

        EventUser saved = eventUserRepository.save(collaborator);
        LocalDateTime invitationTime = Boolean.TRUE.equals(request.getSendInvitation()) ? LocalDateTime.now() : null;
        return toResponse(saved, request.getPermissions(), Boolean.TRUE.equals(request.getSendInvitation()), invitationTime);
    }

    public EventCollaboratorResponse updateCollaborator(UUID eventId, UUID collaboratorId, EventCollaboratorRequest request) {
        EventUser collaborator = eventUserRepository.findById(collaboratorId)
            .orElseThrow(() -> new IllegalArgumentException("Collaborator not found"));

        if (!collaborator.getEventId().equals(eventId)) {
            throw new IllegalArgumentException("Collaborator does not belong to event");
        }

        if (request.getUserId() != null) {
            collaborator.setUserId(request.getUserId());
        }
        if (request.getEmail() != null) {
            collaborator.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            collaborator.setUserType(request.getRole());
        }
        if (request.getNotes() != null) {
            collaborator.setNotes(request.getNotes());
        }

        EventUser saved = eventUserRepository.save(collaborator);
        LocalDateTime invitationTime = Boolean.TRUE.equals(request.getSendInvitation()) ? LocalDateTime.now() : null;
        return toResponse(saved, request.getPermissions(), Boolean.TRUE.equals(request.getSendInvitation()), invitationTime);
    }

    public void removeCollaborator(UUID eventId, UUID collaboratorId) {
        EventUser collaborator = eventUserRepository.findById(collaboratorId)
            .orElseThrow(() -> new IllegalArgumentException("Collaborator not found"));
        if (!collaborator.getEventId().equals(eventId)) {
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
        response.setEventId(eventUser.getEventId());
        response.setUserId(eventUser.getUserId());
        response.setEmail(eventUser.getEmail());
        response.setUserName(eventUser.getName());
        response.setRole(eventUser.getUserType());
        response.setPermissions(permissions != null ? permissions : Collections.emptyList());
        response.setRegistrationStatus(eventUser.getRegistrationStatus() != null ? eventUser.getRegistrationStatus().name() : null);
        response.setNotes(eventUser.getNotes());
        response.setInvitationSent(invitationSent);
        response.setInvitationSentAt(invitationSentAt);
        response.setAddedAt(eventUser.getCreatedAt());
        response.setUpdatedAt(eventUser.getUpdatedAt());
        return response;
    }
}
