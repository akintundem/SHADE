package eventplanner.common.communication.services.core;

import eventplanner.common.communication.services.channel.email.EmailService;
import eventplanner.common.communication.services.channel.email.dto.EmailResponse;
import eventplanner.common.communication.services.channel.push.PushNotificationService;
import eventplanner.common.communication.services.channel.push.dto.PushNotificationRequest;
import eventplanner.common.communication.services.channel.push.dto.PushNotificationResponse;
import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.domain.enums.CommunicationStatus;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.common.domain.enums.RecipientType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final CommunicationRepository communicationRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;

    /**
     * Send notification via email (for event-based notifications)
     */
    public Communication send(UUID eventId, CommunicationType channel, String subject, String content,
                              List<String> recipientEmails, LocalDateTime scheduledAt, String priority) {
        String idempotencyKey = buildIdempotencyKey(eventId, subject, content, recipientEmails);
        log.info("notification_send request eventId={} channel={} scheduledAt={} priority={} idemKey={}",
                eventId, channel, scheduledAt, priority, idempotencyKey);
        
        // Create communication record
        Communication c = new Communication();
        c.setEventId(eventId);
        c.setCommunicationType(channel);
        c.setRecipientType(RecipientType.USER);
        c.setSubject(subject);
        c.setContent(content);
        c.setScheduledAt(scheduledAt);
        c.setStatus(CommunicationStatus.PENDING);
        
        if (recipientEmails != null && !recipientEmails.isEmpty()) {
            c.setRecipientEmail(recipientEmails.get(0));
        }
        
        Communication saved = communicationRepository.save(c);
        
        // If not scheduled, send immediately
        if (scheduledAt == null) {
            try {
                boolean sent = sendViaChannel(saved, recipientEmails, null);
                if (sent) {
                    saved.setStatus(CommunicationStatus.SENT);
                    saved.setSentAt(LocalDateTime.now());
                } else {
                    saved.setStatus(CommunicationStatus.FAILED);
                    saved.setFailedAt(LocalDateTime.now());
                }
                communicationRepository.save(saved);
                log.info("notification_sent eventId={} communicationId={} status={}", 
                        eventId, saved.getId(), saved.getStatus());
            } catch (Exception e) {
                log.error("Failed to send notification", e);
                saved.setStatus(CommunicationStatus.FAILED);
                saved.setFailedAt(LocalDateTime.now());
                saved.setFailureReason(e.getMessage());
                communicationRepository.save(saved);
            }
        }
        
        return saved;
    }

    /**
     * Send email notification (for auth flows and other non-event notifications)
     */
    public Communication sendEmail(String to, String subject, String content, UUID eventId) {
        Communication c = new Communication();
        if (eventId != null) {
            c.setEventId(eventId);
        }
        c.setCommunicationType(CommunicationType.EMAIL);
        c.setRecipientType(RecipientType.USER);
        c.setSubject(subject);
        c.setContent(content);
        c.setRecipientEmail(to);
        c.setStatus(CommunicationStatus.PENDING);
        
        Communication saved = communicationRepository.save(c);
        
        try {
            EmailResponse response = emailService.sendEmail(to, subject, content);
            if (response.isSuccess()) {
                saved.setStatus(CommunicationStatus.SENT);
                saved.setSentAt(LocalDateTime.now());
            } else {
                saved.setStatus(CommunicationStatus.FAILED);
                saved.setFailedAt(LocalDateTime.now());
                saved.setFailureReason(response.getStatusMessage());
            }
            communicationRepository.save(saved);
        } catch (Exception e) {
            log.error("Failed to send email", e);
            saved.setStatus(CommunicationStatus.FAILED);
            saved.setFailedAt(LocalDateTime.now());
            saved.setFailureReason(e.getMessage());
            communicationRepository.save(saved);
        }
        
        return saved;
    }

    /**
     * Send push notification to a user (for auth flows and other non-event notifications)
     */
    public Communication sendPushNotification(UUID userId, String title, String body, 
                                            Map<String, String> data, String actionUrl, UUID eventId) {
        Communication c = new Communication();
        if (eventId != null) {
            c.setEventId(eventId);
        }
        c.setCommunicationType(CommunicationType.PUSH_NOTIFICATION);
        c.setRecipientType(RecipientType.USER);
        c.setRecipientId(userId);
        c.setSubject(title);
        c.setContent(body);
        c.setStatus(CommunicationStatus.PENDING);
        
        // Store actionUrl and data in metadata
        Map<String, Object> metadata = new HashMap<>();
        if (actionUrl != null) {
            metadata.put("actionUrl", actionUrl);
        }
        if (data != null) {
            metadata.put("data", data);
        }
        c.setMetadata(serializeMetadata(metadata));
        
        Communication saved = communicationRepository.save(c);
        
        try {
            PushNotificationRequest request = PushNotificationRequest.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .title(title)
                    .body(body)
                    .data(data)
                    .actionUrl(actionUrl)
                    .build();
            
            PushNotificationResponse response = pushNotificationService.sendToUser(request);
            if (response.isSuccess()) {
                saved.setStatus(CommunicationStatus.SENT);
                saved.setSentAt(LocalDateTime.now());
            } else {
                saved.setStatus(CommunicationStatus.FAILED);
                saved.setFailedAt(LocalDateTime.now());
                saved.setFailureReason(response.getMessage());
            }
            communicationRepository.save(saved);
        } catch (Exception e) {
            log.error("Failed to send push notification", e);
            saved.setStatus(CommunicationStatus.FAILED);
            saved.setFailedAt(LocalDateTime.now());
            saved.setFailureReason(e.getMessage());
            communicationRepository.save(saved);
        }
        
        return saved;
    }

    private boolean sendViaChannel(Communication communication, List<String> recipientEmails, UUID recipientUserId) {
        switch (communication.getCommunicationType()) {
            case EMAIL:
                if (recipientEmails == null || recipientEmails.isEmpty()) {
                    return false;
                }
                return sendEmail(communication, recipientEmails);
            case PUSH_NOTIFICATION:
                if (recipientUserId == null) {
                    log.warn("Push notifications require userId");
                    return false;
                }
                return sendPush(communication, recipientUserId);
            case SMS:
                log.warn("SMS sending not yet implemented");
                return false;
            default:
                log.warn("Unknown communication type: {}", communication.getCommunicationType());
                return false;
        }
    }

    private boolean sendEmail(Communication communication, List<String> recipientEmails) {
        // Send to first recipient (for now - can be extended for bulk)
        String recipient = recipientEmails.get(0);
        EmailResponse response = emailService.sendEmail(
            recipient,
            communication.getSubject(),
            communication.getContent()
        );
        return response.isSuccess();
    }

    private boolean sendPush(Communication communication, UUID userId) {
        Map<String, String> data = parseMetadataData(communication.getMetadata());
        String actionUrl = parseMetadataActionUrl(communication.getMetadata());
        
        PushNotificationRequest request = PushNotificationRequest.builder()
                .userId(userId)
                .eventId(communication.getEventId())
                .title(communication.getSubject())
                .body(communication.getContent())
                .data(data)
                .actionUrl(actionUrl)
                .build();
        
        PushNotificationResponse response = pushNotificationService.sendToUser(request);
        return response.isSuccess();
    }

    private Map<String, String> parseMetadataData(String metadata) {
        // Simple parsing - in production, use proper JSON parsing
        if (metadata == null || !metadata.contains("\"data\"")) {
            return null;
        }
        // For now, return empty map - can be enhanced with proper JSON parsing
        return new HashMap<>();
    }

    private String parseMetadataActionUrl(String metadata) {
        // Simple parsing - in production, use proper JSON parsing
        if (metadata == null || !metadata.contains("\"actionUrl\"")) {
            return null;
        }
        // For now, return null - can be enhanced with proper JSON parsing
        return null;
    }

    private String serializeMetadata(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            StringBuilder builder = new StringBuilder("{");
            payload.forEach((key, value) -> {
                if (builder.length() > 1) {
                    builder.append(',');
                }
                builder.append('"').append(sanitize(key)).append('"').append(':');
                if (value instanceof String) {
                    builder.append('"').append(sanitize(String.valueOf(value))).append('"');
                } else {
                    builder.append('"').append(sanitize(String.valueOf(value))).append('"');
                }
            });
            builder.append('}');
            return builder.toString();
        } catch (Exception ex) {
            return payload.toString();
        }
    }

    private String sanitize(String input) {
        return input.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String buildIdempotencyKey(UUID eventId, String subject, String content, List<String> emails) {
        String base = eventId + "|" + (subject != null ? subject : "") + "|" + 
                     (content != null ? Integer.toString(content.hashCode()) : "") + "|" + 
                     (emails != null ? emails.toString() : "");
        return Integer.toHexString(base.hashCode());
    }
}

