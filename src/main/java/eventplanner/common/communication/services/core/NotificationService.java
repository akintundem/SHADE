package eventplanner.common.communication.services.core;

import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.communication.services.channel.email.EmailService;
import eventplanner.common.communication.services.channel.email.dto.EmailResult;
import eventplanner.common.communication.services.channel.push.PushNotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.services.core.dto.NotificationResponse;
import eventplanner.common.communication.services.core.dto.PushResult;
import eventplanner.common.domain.enums.CommunicationStatus;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.common.communication.model.CommunicationRecipientType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
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
     * @return NotificationResponse indicating success or failure with details
     */
    public NotificationResponse send(NotificationRequest request) {
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
                    return NotificationResponse.success(
                            comm.getId(),
                            comm.getExternalId(),
                            comm.getStatus()
                    );
                }
            }
        }

        // Create and save communication record
        Communication communication = new Communication();
        if (eventId != null) {
            communication.setEventId(eventId);
        }
        communication.setCommunicationType(type);
        communication.setRecipientType(CommunicationRecipientType.USER);
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
                // Create communication record with failed status immediately
                communication.setStatus(CommunicationStatus.FAILED);
                communication.setFailedAt(LocalDateTime.now());
                communication.setFailureReason("Invalid userId format: " + to);
                communication.setChannel("push");
                Communication saved = communicationRepository.save(communication);
                return NotificationResponse.failure(saved.getId(), "Invalid userId format: " + to, saved.getStatus());
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
                    String from = request.getFrom();
                    if (from == null || from.isBlank()) {
                        throw new IllegalArgumentException("From address is required for email notifications");
                    }
                    EmailResult emailResult = sendEmail(to, subject, templateId, templateVariables, from);
                    success = emailResult.isSuccess();
                    externalId = emailResult.getMessageId();
                    errorMessage = emailResult.getErrorMessage();
                    break;
                case PUSH_NOTIFICATION:
                    // Convert to Map<String, String> for push notifications
                    Map<String, String> pushData = new HashMap<>();
                    if (templateVariables != null) {
                        for (Map.Entry<String, Object> entry : templateVariables.entrySet()) {
                            pushData.put(entry.getKey(), String.valueOf(entry.getValue()));
                        }
                    }
                    // Add eventId to data if provided
                    if (eventId != null) {
                        pushData.put("eventId", eventId.toString());
                    }
                    // Extract body from data if available
                    String body = pushData.remove("body");
                    
                    PushResult pushResult = sendPushNotification(to, subject, body, pushData);
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
                communicationRepository.save(saved);
                return NotificationResponse.success(saved.getId(), externalId, saved.getStatus());
            } else {
                saved.setStatus(CommunicationStatus.FAILED);
                saved.setFailedAt(LocalDateTime.now());
                if (errorMessage != null) {
                    saved.setFailureReason(errorMessage);
                }
                communicationRepository.save(saved);
                return NotificationResponse.failure(saved.getId(), errorMessage, saved.getStatus());
            }
            
        } catch (Exception e) {
            // Update communication as failed
            saved.setStatus(CommunicationStatus.FAILED);
            saved.setFailedAt(LocalDateTime.now());
            saved.setFailureReason(e.getMessage());
            communicationRepository.save(saved);
            
            log.error("Failed to send communication: {}", e.getMessage(), e);
            return NotificationResponse.failure(saved.getId(), e.getMessage(), saved.getStatus());
        }
    }

    /**
     * Send email via EmailService
     */
    private EmailResult sendEmail(String to, String subject, String templateId,
                                  Map<String, Object> templateVariables, String from) {
        return emailService.sendEmail(to, subject, from, templateId, templateVariables);
    }

    /**
     * Send push notification via PushNotificationService
     */
    private PushResult sendPushNotification(String userIdString, String title, String body, 
                                           Map<String, String> data) {
        try {
            UUID userId = UUID.fromString(userIdString);
            return pushNotificationService.sendToNotification(userId, title, body, data);
        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format for push notification: {}", userIdString, e);
            return PushResult.builder()
                    .success(false)
                    .errorMessage("Invalid userId format: " + userIdString)
                    .build();
        }
    }
}
