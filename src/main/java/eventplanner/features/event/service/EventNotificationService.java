package eventplanner.features.event.service;

import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.domain.enums.CommunicationStatus;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.features.event.dto.request.EventNotificationRequest;
import eventplanner.features.event.dto.response.EventNotificationResponse;
import eventplanner.features.event.entity.EventNotificationSettings;
import eventplanner.features.event.enums.EventNotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EventNotificationService {

    private final NotificationService notificationService;
    private final EventNotificationSettingsService settingsService;
    private final CommunicationRepository communicationRepository;
    private final EventRecipientResolverService recipientResolverService;

    public EventNotificationResponse sendNotification(UUID eventId, EventNotificationRequest request) {
        EventNotificationSettings settings = settingsService.getSettingsEntity(eventId);
        validateChannelEnabled(settings, request.getChannel());

        CommunicationType type = request.getChannel().toCommunicationType();
        
        // Resolve recipients using the recipient resolver service
        EventRecipientResolverService.RecipientInfo recipients = recipientResolverService.resolveRecipients(
            eventId,
            request.getRecipientTypes(),
            request.getRecipientUserIds(),
            request.getRecipientEmails()
        );

        List<String> successfulRecipients = new ArrayList<>();
        List<String> failedRecipients = new ArrayList<>();
        int totalSent = 0;

        // Prepare template variables
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("content", request.getContent());
        if (request.getIncludeEventDetails() != null && request.getIncludeEventDetails()) {
            templateVariables.put("includeEventDetails", true);
        }

        // Send to email recipients
        if (type == CommunicationType.EMAIL && !recipients.getEmails().isEmpty()) {
            for (String email : recipients.getEmails()) {
                try {
                    NotificationRequest notificationRequest = NotificationRequest.builder()
                            .type(type)
                            .to(email)
                            .subject(request.getSubject())
                            .templateId(request.getTemplateId())
                            .templateVariables(templateVariables)
                            .eventId(eventId)
                            .build();
                    
                    notificationService.send(notificationRequest);
                    successfulRecipients.add(email);
                    totalSent++;
                } catch (Exception e) {
                    log.error("Failed to send notification to email: {}", email, e);
                    failedRecipients.add(email);
                }
            }
        }

        // Send to user IDs (for push notifications or when user IDs are available)
        if ((type == CommunicationType.PUSH_NOTIFICATION || type == CommunicationType.EMAIL) 
                && !recipients.getUserIds().isEmpty()) {
            for (UUID userId : recipients.getUserIds()) {
                try {
                    Map<String, Object> data = new HashMap<>(templateVariables);
                    if (type == CommunicationType.PUSH_NOTIFICATION) {
                        data.put("body", request.getContent());
                    }
                    
                    NotificationRequest notificationRequest = NotificationRequest.builder()
                            .type(type)
                            .to(userId.toString())
                            .subject(request.getSubject())
                            .templateId(type == CommunicationType.PUSH_NOTIFICATION ? null : request.getTemplateId())
                            .templateVariables(data)
                            .eventId(eventId)
                            .build();
                    
                    notificationService.send(notificationRequest);
                    successfulRecipients.add(userId.toString());
                    totalSent++;
                } catch (Exception e) {
                    log.error("Failed to send notification to user: {}", userId, e);
                    failedRecipients.add(userId.toString());
                }
            }
        }

        log.info("Sent {} notifications for event {}: {} successful, {} failed", 
                totalSent, eventId, successfulRecipients.size(), failedRecipients.size());

        // Get a sample communication for response (latest one)
        Communication communication = communicationRepository
                .findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .filter(c -> c.getCommunicationType() == type)
                .findFirst()
                .orElse(null);

        return toResponse(eventId, request, communication, recipients, successfulRecipients, failedRecipients);
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

    private EventNotificationResponse toResponse(UUID eventId, 
                                                 EventNotificationRequest request, 
                                                 Communication communication,
                                                 EventRecipientResolverService.RecipientInfo recipients,
                                                 List<String> successfulRecipients,
                                                 List<String> failedRecipients) {
        EventNotificationResponse response = new EventNotificationResponse();
        if (communication != null) {
            response.setNotificationId(communication.getId());
            response.setStatus(communication.getStatus() != null ? communication.getStatus().name().toLowerCase() : null);
            response.setSentAt(communication.getSentAt());
            response.setCreatedAt(communication.getCreatedAt());
        } else {
            // No communication record found - notification may have failed or not been persisted
            response.setNotificationId(null);
            response.setStatus(successfulRecipients.isEmpty() ? "failed" : "partial");
            response.setSentAt(null);
            response.setCreatedAt(null);
        }
        response.setEventId(eventId);
        response.setChannel(request.getChannel());
        response.setSubject(request.getSubject());
        response.setContent(request.getContent());
        response.setRecipientCount(recipients.getTotalCount());
        response.setScheduledAt(request.getScheduledAt());
        response.setPriority(request.getPriority());
        response.setSuccessfulRecipients(successfulRecipients);
        response.setFailedRecipients(failedRecipients);
        return response;
    }
}
