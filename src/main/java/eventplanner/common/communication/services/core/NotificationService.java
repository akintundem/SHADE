package eventplanner.common.communication.services.core;

import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.communication.services.channel.email.EmailService;
import eventplanner.common.communication.services.channel.email.dto.EmailResult;
import eventplanner.common.communication.services.channel.push.PushNotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.services.core.dto.PushResult;
import eventplanner.common.domain.enums.CommunicationStatus;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.common.domain.enums.RecipientType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Notification service for sending communications via different channels
 * Supports: Email (via templates) and Push Notification
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;
    private final CommunicationRepository communicationRepository;

    /**
     * Send communication based on type
     * Creates and saves communication record, then sends via appropriate channel
     * 
     * @param request Notification request containing all necessary information
     */
    public void send(NotificationRequest request) {
        CommunicationType type = request.getType();
        String to = request.getTo();
        String subject = request.getSubject();
        String templateId = request.getTemplateId();
        Map<String, Object> templateVariables = request.getTemplateVariables() != null 
                ? request.getTemplateVariables() 
                : new java.util.HashMap<>();
        UUID eventId = request.getEventId();
        
        // Check idempotency for EMAIL type
        if (type == CommunicationType.EMAIL && templateId != null) {
            Optional<Communication> existing = communicationRepository
                    .findFirstByRecipientEmailAndTemplateIdAndSubjectAndStatusOrderByCreatedAtDesc(
                            to, templateId, subject, CommunicationStatus.SENT);
            
            if (existing.isPresent()) {
                Communication comm = existing.get();
                if (comm.getSentAt() != null && 
                    comm.getSentAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
                    log.info("Communication already sent recently (idempotency check). Skipping duplicate send to: {}, template: {}", to, templateId);
                    return;
                }
            }
        }

        // Create and save communication record
        Communication communication = new Communication();
        if (eventId != null) {
            communication.setEventId(eventId);
        }
        communication.setCommunicationType(type);
        communication.setRecipientType(RecipientType.USER);
        communication.setSubject(subject);
        communication.setStatus(CommunicationStatus.PENDING);
        
        // Set channel-specific fields
        if (type == CommunicationType.EMAIL) {
            communication.setRecipientEmail(to);
            communication.setTemplateId(templateId);
            communication.setContent("Template: " + templateId + " with variables: " + 
                    (templateVariables != null ? templateVariables.toString() : "none"));
            communication.setChannel("email");
        } else if (type == CommunicationType.PUSH_NOTIFICATION) {
            try {
                UUID userId = UUID.fromString(to);
                communication.setRecipientId(userId);
            } catch (IllegalArgumentException e) {
                log.error("Invalid userId format for push notification: {}", to);
                throw new IllegalArgumentException("Invalid userId format: " + to);
            }
            communication.setContent(subject); // Use subject as body for push
            communication.setChannel("push");
        }
        
        Communication saved = communicationRepository.save(communication);
        
        try {
            // Send via appropriate channel
            boolean success = false;
            String externalId = null;
            String errorMessage = null;
            
            switch (type) {
                case EMAIL:
                    EmailResult emailResult = sendEmail(to, subject, templateId, templateVariables);
                    success = emailResult.isSuccess();
                    externalId = emailResult.getMessageId();
                    errorMessage = emailResult.getErrorMessage();
                    break;
                case PUSH_NOTIFICATION:
                    PushResult pushResult = sendPushNotification(to, subject, templateVariables, eventId);
                    success = pushResult.isSuccess();
                    externalId = pushResult.getMessageId();
                    errorMessage = pushResult.getErrorMessage();
                    break;
                default:
                    throw new IllegalArgumentException("Communication type not supported: " + type);
            }
            
            // Update communication based on result
            if (success) {
                saved.setStatus(CommunicationStatus.SENT);
                saved.setSentAt(LocalDateTime.now());
                if (externalId != null) {
                    saved.setExternalId(externalId);
                }
            } else {
                saved.setStatus(CommunicationStatus.FAILED);
                saved.setFailedAt(LocalDateTime.now());
                if (errorMessage != null) {
                    saved.setFailureReason(errorMessage);
                }
            }
            communicationRepository.save(saved);
            
        } catch (Exception e) {
            // Update communication as failed
            saved.setStatus(CommunicationStatus.FAILED);
            saved.setFailedAt(LocalDateTime.now());
            saved.setFailureReason(e.getMessage());
            communicationRepository.save(saved);
            
            log.error("Failed to send communication: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Send email via EmailService
     */
    private EmailResult sendEmail(String to, String subject, String templateId,
                                  Map<String, Object> templateVariables) {
        return emailService.sendEmail(to, subject, templateId, templateVariables);
    }

    /**
     * Send push notification via PushNotificationService
     */
    private PushResult sendPushNotification(String userIdString, String title, 
                                           Map<String, Object> data, UUID eventId) {
        try {
            UUID userId = UUID.fromString(userIdString);
            return pushNotificationService.sendPushNotification(userId, title, data, eventId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format for push notification: {}", userIdString, e);
            return PushResult.builder()
                    .success(false)
                    .errorMessage("Invalid userId format: " + userIdString)
                    .build();
        }
    }
}
