package eventplanner.common.communication.services.core;

import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.model.CommunicationRecipientType;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.communication.services.channel.push.PushNotificationService;
import eventplanner.common.communication.services.core.dto.BulkNotificationRequest;
import eventplanner.common.communication.services.core.dto.BulkNotificationResponse;
import eventplanner.common.communication.services.core.dto.NotificationTarget;
import eventplanner.common.communication.enums.CommunicationStatus;
import eventplanner.common.communication.enums.CommunicationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Internal service for sending bulk notifications to multiple recipients.
 * 
 * <p>This service is <strong>internal-only</strong> and should not be exposed via REST API.
 * It should be called internally by other services such as:
 * <ul>
 *   <li>Event services (for event-related notifications)</li>
 *   <li>Scheduled reminder services</li>
 *   <li>Background job processors</li>
 * </ul>
 * 
 * <p>Handles batching, error recovery, and token management for bulk operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BulkNotificationService {

    private final NotificationTargetResolver targetResolver;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;
    private final CommunicationRepository communicationRepository;
    
    @Value("${external.email.from:Shade <noreply@shade.com>}")
    private String defaultFrom;

    /**
     * Send bulk push notifications to a target group
     * 
     * @param request Bulk notification request (must not be null, targetType and title must be provided)
     * @return BulkNotificationResponse with send results
     */
    public BulkNotificationResponse sendBulkPushNotification(BulkNotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("BulkNotificationRequest cannot be null");
        }
        if (request.getTargetType() == null) {
            throw new IllegalArgumentException("Target type is required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        
        BulkNotificationResponse response = new BulkNotificationResponse();
        
        try {
            // Resolve target recipients
            NotificationTarget target = targetResolver.resolveTarget(
                    request.getTargetType(),
                    request.getEventId(),
                    request.getParameters() != null ? request.getParameters() : new HashMap<>()
            );
            
            if (target.isEmpty()) {
                log.warn("No recipients found for target type: {}", request.getTargetType());
                response.setSuccess(false);
                response.setErrorMessage("No recipients found for the specified target");
                response.setTotalRecipients(0);
                return response;
            }
            
            log.info("Sending bulk push notification to {} recipients ({} user IDs, {} emails)", 
                    target.getTotalCount(), target.getUserIds().size(), target.getEmails().size());
            
            // Send push notifications to users with device tokens
            if (!target.getUserIds().isEmpty()) {
                List<UUID> userIds = new ArrayList<>(target.getUserIds());
                PushNotificationService.BulkPushResult pushResult = pushNotificationService.sendBulkNotification(
                        userIds,
                        request.getTitle(),
                        request.getBody(),
                        request.getData()
                );
                
                response.setTotalRecipients(pushResult.getTotalRecipients());
                response.setSuccessCount(pushResult.getSuccessCount());
                response.setFailureCount(pushResult.getFailureCount());
                response.setMessageId(pushResult.getMessageId());
                
                if (pushResult.getErrorMessage() != null) {
                    response.setErrorMessage(pushResult.getErrorMessage());
                }
            } else {
                log.warn("No user IDs found for push notification. Only emails available: {}", target.getEmails().size());
                response.setTotalRecipients(0);
                response.setSuccessCount(0);
                response.setFailureCount(0);
            }
            
            // Create communication records for tracking
            createBulkCommunicationRecords(
                    request.getEventId(),
                    CommunicationType.PUSH_NOTIFICATION,
                    target,
                    request.getTitle(),
                    response
            );
            
            response.setSuccess(response.getFailureCount() == 0 && response.getSuccessCount() > 0);
            
            log.info("Bulk push notification completed: {} successful, {} failed out of {} total", 
                    response.getSuccessCount(), response.getFailureCount(), response.getTotalRecipients());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error sending bulk push notification", e);
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
            return response;
        }
    }

    /**
     * Send bulk email notifications to a target group
     * 
     * @param request Bulk notification request (must not be null, targetType, title, and templateId must be provided)
     * @return BulkNotificationResponse with send results
     */
    public BulkNotificationResponse sendBulkEmailNotification(BulkNotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("BulkNotificationRequest cannot be null");
        }
        if (request.getTargetType() == null) {
            throw new IllegalArgumentException("Target type is required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (request.getTemplateId() == null || request.getTemplateId().isBlank()) {
            throw new IllegalArgumentException("Template ID is required for email notifications");
        }
        
        BulkNotificationResponse response = new BulkNotificationResponse();
        
        try {
            // Resolve target recipients
            NotificationTarget target = targetResolver.resolveTarget(
                    request.getTargetType(),
                    request.getEventId(),
                    request.getParameters() != null ? request.getParameters() : new HashMap<>()
            );
            
            if (target.isEmpty()) {
                log.warn("No recipients found for target type: {}", request.getTargetType());
                response.setSuccess(false);
                response.setErrorMessage("No recipients found for the specified target");
                response.setTotalRecipients(0);
                return response;
            }
            
            log.info("Sending bulk email notification to {} recipients", target.getTotalCount());
            
            int successCount = 0;
            int failureCount = 0;
            Set<String> successfulEmails = new HashSet<>();
            Set<String> failedEmails = new HashSet<>();
            
            // Send emails to all recipients (both user emails and guest emails)
            Set<String> allEmails = new HashSet<>(target.getEmails());
            
            // Emails from user IDs are already included by the target resolver
            
            String from = request.getFrom() != null ? request.getFrom() : defaultFrom;
            
            for (String email : allEmails) {
                try {
                    var notificationRequest = eventplanner.common.communication.services.core.dto.NotificationRequest.builder()
                            .type(CommunicationType.EMAIL)
                            .to(email)
                            .subject(request.getTitle())
                            .templateId(request.getTemplateId())
                            .templateVariables(request.getData() != null ? 
                                    request.getData().entrySet().stream()
                                            .collect(Collectors.toMap(
                                                    Map.Entry::getKey,
                                                    e -> (Object) e.getValue()
                                            )) : new HashMap<>())
                            .eventId(request.getEventId())
                            .from(from)
                            .build();
                    
                    var notificationResponse = notificationService.send(notificationRequest);
                    
                    if (notificationResponse.isSuccess()) {
                        successCount++;
                        successfulEmails.add(email);
                    } else {
                        failureCount++;
                        failedEmails.add(email);
                        log.warn("Failed to send email to {}: {}", email, notificationResponse.getErrorMessage());
                    }
                } catch (Exception e) {
                    failureCount++;
                    failedEmails.add(email);
                    log.error("Exception sending email to {}: {}", email, e.getMessage(), e);
                }
            }
            
            response.setTotalRecipients(allEmails.size());
            response.setSuccessCount(successCount);
            response.setFailureCount(failureCount);
            response.setSuccess(failureCount == 0 && successCount > 0);
            
            // Create communication records
            createBulkCommunicationRecords(
                    request.getEventId(),
                    CommunicationType.EMAIL,
                    target,
                    request.getTitle(),
                    response
            );
            
            log.info("Bulk email notification completed: {} successful, {} failed out of {} total", 
                    successCount, failureCount, allEmails.size());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error sending bulk email notification", e);
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
            return response;
        }
    }

    /**
     * Create communication records for bulk notification tracking
     */
    private void createBulkCommunicationRecords(UUID eventId, CommunicationType type, 
                                                NotificationTarget target, String subject,
                                                BulkNotificationResponse response) {
        // Create a summary communication record
        Communication communication = new Communication();
        communication.setEventId(eventId);
        communication.setCommunicationType(type);
        communication.setRecipientType(CommunicationRecipientType.BULK);
        communication.setSubject(subject);
        communication.setStatus(response.isSuccess() ? CommunicationStatus.SENT : CommunicationStatus.FAILED);
        communication.setChannel(type == CommunicationType.PUSH_NOTIFICATION ? "push" : "email");
        communication.setContent(String.format("Bulk notification to %d recipients. Success: %d, Failed: %d", 
                response.getTotalRecipients(), response.getSuccessCount(), response.getFailureCount()));
        
        if (response.isSuccess()) {
            communication.setSentAt(LocalDateTime.now());
            if (response.getMessageId() != null) {
                communication.setExternalId(response.getMessageId());
            }
        } else {
            communication.setFailedAt(LocalDateTime.now());
            communication.setFailureReason(response.getErrorMessage());
        }
        
        communicationRepository.save(communication);
    }
}

