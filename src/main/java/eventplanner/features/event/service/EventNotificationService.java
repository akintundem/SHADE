package eventplanner.features.event.service;

import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.services.core.dto.NotificationResponse;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.features.event.dto.request.EventNotificationRequest;
import eventplanner.features.event.dto.response.EventNotificationResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventNotificationSettings;
import eventplanner.features.event.enums.EventNotificationChannel;
import eventplanner.features.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EventNotificationService {

    private final NotificationService notificationService;
    private final EventNotificationSettingsService settingsService;
    private final CommunicationRepository communicationRepository;
    private final EventRecipientResolverService recipientResolverService;
    private final EventEmailTemplateService emailTemplateService;
    private final EventRepository eventRepository;
    private final EventTemplateVariableService templateVariableService;

    public EventNotificationResponse sendNotification(UUID eventId, EventNotificationRequest request) {
        EventNotificationSettings settings = settingsService.getSettingsEntity(eventId);
        validateChannelEnabled(settings, request.getChannel());

        CommunicationType type = request.getChannel().toCommunicationType();
        
        // Validate email template type for EMAIL channel
        String templateId = resolveTemplateId(request, type);
        log.info("Resolved template ID: {} for template type: {}", templateId, request.getEmailTemplateType());
        
        // Resolve recipients using the recipient resolver service
        EventRecipientResolverService.RecipientInfo recipients = recipientResolverService.resolveRecipients(
            eventId,
            request.getRecipientTypes(),
            request.getRecipientUserIds(),
            request.getRecipientEmails()
        );

        log.info("Resolved {} recipients for event {}: {} emails, {} user IDs", 
                recipients.getTotalCount(), eventId, recipients.getEmails().size(), recipients.getUserIds().size());

        List<String> successfulRecipients = new ArrayList<>();
        List<String> failedRecipients = new ArrayList<>();

        // Fetch event details for template variables
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        // Prepare template variables with event details using shared service
        Map<String, Object> templateVariables = templateVariableService.prepareTemplateVariables(
            event, 
            request.getContent(), 
            request.getSubject(), 
            request.getEmailTemplateType()
        );
        log.debug("Prepared {} template variables for event {}", templateVariables.size(), eventId);

        // Send to email recipients
        if (type == CommunicationType.EMAIL && !recipients.getEmails().isEmpty()) {
            for (String email : recipients.getEmails()) {
                try {
                    log.info("Sending email notification to: {} using template: {}", email, templateId);
                    NotificationRequest notificationRequest = NotificationRequest.builder()
                            .type(type)
                            .to(email)
                            .subject(request.getSubject())
                            .templateId(templateId)
                            .templateVariables(templateVariables)
                            .eventId(eventId)
                            .build();
                    
                    NotificationResponse response = notificationService.send(notificationRequest);
                    
                    if (response.isSuccess()) {
                        successfulRecipients.add(email);
                        log.info("Successfully sent email notification to: {} - Message ID: {}", email, response.getMessageId());
                    } else {
                        failedRecipients.add(email);
                        log.error("Failed to send email notification to: {} - Error: {}", email, response.getErrorMessage());
                    }
                } catch (Exception e) {
                    log.error("Exception while sending notification to email: {} - Error: {}", email, e.getMessage(), e);
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
                            .templateId(type == CommunicationType.PUSH_NOTIFICATION ? null : templateId)
                            .templateVariables(data)
                            .eventId(eventId)
                            .build();
                    
                    NotificationResponse response = notificationService.send(notificationRequest);
                    
                    if (response.isSuccess()) {
                        successfulRecipients.add(userId.toString());
                        log.info("Successfully sent notification to user: {} - Message ID: {}", userId, response.getMessageId());
                    } else {
                        failedRecipients.add(userId.toString());
                        log.error("Failed to send notification to user: {} - Error: {}", userId, response.getErrorMessage());
                    }
                } catch (Exception e) {
                    log.error("Exception while sending notification to user: {} - Error: {}", userId, e.getMessage(), e);
                    failedRecipients.add(userId.toString());
                }
            }
        }

        log.info("Notification sending complete for event {}: {} successful, {} failed", 
                eventId, successfulRecipients.size(), failedRecipients.size());
        
        // If all notifications failed, throw an exception
        if (successfulRecipients.isEmpty() && !failedRecipients.isEmpty()) {
            String errorMsg = String.format("All notifications failed to send. Errors: %s", 
                    String.join(", ", failedRecipients));
            log.error(errorMsg);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
        
        // If some failed, log warning but don't throw (partial success)
        if (!failedRecipients.isEmpty()) {
            log.warn("Some notifications failed: {}", failedRecipients);
        }

        // Get a sample communication for response (latest one)
        Communication communication = communicationRepository
                .findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .filter(c -> c.getCommunicationType() == type)
                .findFirst()
                .orElse(null);

        return toResponse(eventId, request, communication, recipients, successfulRecipients, failedRecipients);
    }

    /**
     * Resolve the Resend template ID from the request
     * Priority: custom templateId > emailTemplateType mapping
     * 
     * @param request The notification request
     * @param type The communication type
     * @return Resend template ID string, or null if not applicable
     */
    private String resolveTemplateId(EventNotificationRequest request, CommunicationType type) {
        // If custom templateId is provided, use it (takes priority)
        if (request.getTemplateId() != null && !request.getTemplateId().isBlank()) {
            return request.getTemplateId();
        }
        
        // For EMAIL channel, require emailTemplateType
        if (type == CommunicationType.EMAIL) {
            if (request.getEmailTemplateType() == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, 
                    "emailTemplateType is required for EMAIL channel. Options: ANNOUNCEMENT, CANCEL_EVENT"
                );
            }
            return emailTemplateService.getTemplateId(request.getEmailTemplateType());
        }
        
        // For other channels, no template needed
        return null;
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
