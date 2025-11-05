package eventplanner.features.event.service;

import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.features.event.dto.request.EventNotificationRequest;
import eventplanner.features.event.dto.response.EventNotificationResponse;
import eventplanner.features.event.entity.EventNotificationSettings;
import eventplanner.features.event.enums.EventNotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EventNotificationService {

    private final NotificationService notificationService;
    private final EventNotificationSettingsService settingsService;
    private final CommunicationRepository communicationRepository;

    public EventNotificationResponse sendNotification(UUID eventId, EventNotificationRequest request) {
        EventNotificationSettings settings = settingsService.getSettingsEntity(eventId);
        validateChannelEnabled(settings, request.getChannel());

        CommunicationType type = request.getChannel().toCommunicationType();
        List<Communication> communications = new ArrayList<>();
        
        // Send to all recipients
        if (type == CommunicationType.EMAIL && request.getRecipientEmails() != null) {
            for (String email : request.getRecipientEmails()) {
                Map<String, Object> templateVariables = new HashMap<>();
                templateVariables.put("content", request.getContent());
                
                NotificationRequest notificationRequest = NotificationRequest.builder()
                        .type(type)
                        .to(email)
                        .subject(request.getSubject())
                        .templateId(request.getTemplateId())
                        .templateVariables(templateVariables)
                        .eventId(eventId)
                        .build();
                
                notificationService.send(notificationRequest);
                
                // Get the latest communication for this recipient
                Communication comm = communicationRepository
                        .findFirstByRecipientEmailAndTemplateIdAndSubjectAndStatusOrderByCreatedAtDesc(
                                email, request.getTemplateId(), request.getSubject(), 
                                eventplanner.common.domain.enums.CommunicationStatus.SENT)
                        .orElse(null);
                if (comm != null) {
                    communications.add(comm);
                }
            }
        } else if (type == CommunicationType.PUSH_NOTIFICATION && request.getRecipientUserIds() != null) {
            for (UUID userId : request.getRecipientUserIds()) {
                Map<String, Object> data = new HashMap<>();
                data.put("body", request.getContent());
                
                NotificationRequest notificationRequest = NotificationRequest.builder()
                        .type(type)
                        .to(userId.toString())
                        .subject(request.getSubject())
                        .templateId(null)
                        .templateVariables(data)
                        .eventId(eventId)
                        .build();
                
                notificationService.send(notificationRequest);
                
                // Get the latest communication for this user
                Communication comm = communicationRepository
                        .findByEventIdOrderByCreatedAtDesc(eventId)
                        .stream()
                        .filter(c -> c.getRecipientId() != null && c.getRecipientId().equals(userId))
                        .findFirst()
                        .orElse(null);
                if (comm != null) {
                    communications.add(comm);
                }
            }
        }
        
        // Use first communication for response (or find latest by eventId)
        Communication communication = communications.isEmpty() 
                ? communicationRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                        .stream()
                        .filter(c -> c.getCommunicationType() == type)
                        .findFirst()
                        .orElse(null)
                : communications.get(0);

        return toResponse(eventId, request, communication);
    }

    private void validateChannelEnabled(EventNotificationSettings settings, EventNotificationChannel channel) {
        if (channel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification channel is required");
        }
        boolean enabled = switch (channel) {
            case EMAIL -> Boolean.TRUE.equals(settings.getEmailEnabled());
            case SMS -> Boolean.TRUE.equals(settings.getSmsEnabled());
            case PUSH -> Boolean.TRUE.equals(settings.getPushEnabled());
        };
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification channel is disabled for this event");
        }
    }

    private EventNotificationResponse toResponse(UUID eventId, EventNotificationRequest request, Communication communication) {
        EventNotificationResponse response = new EventNotificationResponse();
        response.setNotificationId(communication.getId());
        response.setEventId(eventId);
        response.setChannel(request.getChannel());
        response.setSubject(request.getSubject());
        response.setContent(request.getContent());
        response.setStatus(communication.getStatus() != null ? communication.getStatus().name().toLowerCase() : null);
        response.setRecipientCount(calculateRecipientCount(request));
        response.setScheduledAt(request.getScheduledAt());
        response.setSentAt(communication.getSentAt());
        response.setPriority(request.getPriority());
        response.setCreatedAt(communication.getCreatedAt());
        response.setSuccessfulRecipients(request.getRecipientEmails() != null ? request.getRecipientEmails() : new ArrayList<>());
        response.setFailedRecipients(new ArrayList<>());
        return response;
    }

    private int calculateRecipientCount(EventNotificationRequest request) {
        int emails = request.getRecipientEmails() != null ? request.getRecipientEmails().size() : 0;
        int users = request.getRecipientUserIds() != null ? request.getRecipientUserIds().size() : 0;
        return emails + users;
    }
}
