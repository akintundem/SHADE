package eventplanner.features.collaboration.service;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.enums.CommunicationType;
import eventplanner.features.collaboration.enums.RegistrationStatus;
import eventplanner.features.collaboration.dto.request.EventCollaboratorRequest;
import eventplanner.features.collaboration.dto.response.EventCollaboratorResponse;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.entity.EventUserPermission;
import eventplanner.features.collaboration.enums.EventPermission;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.common.config.ExternalServicesProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class EventCollaboratorService {

    private final EventUserRepository eventUserRepository;
    private final EventRepository eventRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;
    private final ExternalServicesProperties externalServicesProperties;

    public EventCollaboratorService(EventUserRepository eventUserRepository, 
                                   EventRepository eventRepository,
                                   UserAccountRepository userAccountRepository,
                                   NotificationService notificationService,
                                   ExternalServicesProperties externalServicesProperties) {
        this.eventUserRepository = eventUserRepository;
        this.eventRepository = eventRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
        this.externalServicesProperties = externalServicesProperties;
    }

    public List<EventCollaboratorResponse> getCollaborators(UUID eventId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return eventUserRepository.findByEventId(eventId, PageRequest.of(safePage, safeSize)).getContent().stream()
                .map(eventUser -> toResponse(eventUser, false, null))
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
        updatePermissions(collaborator, request.getPermissions());

        EventUser saved = eventUserRepository.save(collaborator);
        
        // Send notification to the newly added collaborator
        sendCollaboratorWelcomeCommunication(event, saved);
        
        return toResponse(saved, false, null);
    }
    
    /**
     * Send welcome communication to newly added collaborator
     */
    private void sendCollaboratorWelcomeCommunication(Event event, EventUser collaborator) {
        if (event == null || collaborator == null || collaborator.getUser() == null) {
            return;
        }
        
        try {
            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("eventName", event.getName());
            templateVariables.put("eventId", event.getId().toString());
            templateVariables.put("role", collaborator.getUserType() != null ? collaborator.getUserType().name() : "COLLABORATOR");
            if (event.getStartDateTime() != null) {
                templateVariables.put("eventDate", event.getStartDateTime().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a")));
            }
            if (event.getEventWebsiteUrl() != null) {
                templateVariables.put("actionUrl", event.getEventWebsiteUrl());
            }
            templateVariables.put("collaboratorName", collaborator.getUser() != null ? collaborator.getUser().getName() : "there");
            
            UserAccount user = collaborator.getUser();
            
            // Send email if available
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                notificationService.send(NotificationRequest.builder()
                        .type(CommunicationType.EMAIL)
                        .to(user.getEmail())
                        .subject("You've been added as a collaborator: " + event.getName())
                        .templateId("collaborator-welcome")
                        .templateVariables(templateVariables)
                        .eventId(event.getId())
                        .from(externalServicesProperties.getEmail().getFromEvents())
                        .build());
            }
            
            // Send push notification
            Map<String, Object> pushData = new HashMap<>(templateVariables);
            pushData.put("body", "You've been added as a collaborator to: " + event.getName());
            
            notificationService.send(NotificationRequest.builder()
                    .type(CommunicationType.PUSH_NOTIFICATION)
                    .to(user.getId().toString())
                    .subject("Event collaboration confirmed")
                    .templateVariables(pushData)
                    .eventId(event.getId())
                    .build());
        } catch (Exception e) {
            // Don't fail the entire operation if communication fails
        }
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
        if (request != null && request.getPermissions() != null) {
            updatePermissions(collaborator, request.getPermissions());
        }

        EventUser saved = eventUserRepository.save(collaborator);
        return toResponse(saved, false, null);
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
                                                 boolean invitationSent,
                                                 LocalDateTime invitationSentAt) {
        EventCollaboratorResponse response = new EventCollaboratorResponse();
        response.setCollaboratorId(eventUser.getId());
        response.setEventId(eventUser.getEvent() != null ? eventUser.getEvent().getId() : null);
        response.setUserId(eventUser.getUser() != null ? eventUser.getUser().getId() : null);
        response.setEmail(eventUser.getUser() != null ? eventUser.getUser().getEmail() : null);
        response.setUserName(eventUser.getUser() != null ? eventUser.getUser().getName() : null);
        response.setRole(eventUser.getUserType());
        response.setPermissions(eventUser.getPermissions() != null
                ? eventUser.getPermissions().stream()
                    .map(EventUserPermission::getPermission)
                    .filter(perm -> perm != null)
                    .toList()
                : Collections.emptyList());
        response.setRegistrationStatus(eventUser.getRegistrationStatus() != null ? eventUser.getRegistrationStatus().name() : null);
        response.setInvitationSent(invitationSent);
        response.setInvitationSentAt(invitationSentAt);
        response.setAddedAt(eventUser.getCreatedAt());
        response.setUpdatedAt(eventUser.getUpdatedAt());
        return response;
    }

    private void updatePermissions(EventUser collaborator, List<EventPermission> permissions) {
        if (collaborator.getPermissions() == null) {
            collaborator.setPermissions(new java.util.ArrayList<>());
        } else {
            collaborator.getPermissions().clear();
        }
        if (permissions == null) {
            return;
        }
        permissions.stream()
                .filter(p -> p != null)
                .forEach(p -> collaborator.getPermissions().add(new EventUserPermission(collaborator, p)));
    }
}
