package ai.eventplanner.comms.service;

import ai.eventplanner.comms.model.CommunicationLogEntity;
import ai.eventplanner.comms.repository.CommunicationLogRepository;
import ai.eventplanner.common.domain.enums.CommunicationStatus;
import ai.eventplanner.sendgrid.dto.EmailResponse;
import ai.eventplanner.sendgrid.service.SendGridService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Template email service using SendGrid with optional logging
 * Focused on sending beautifully designed template emails
 */
@Service
public class EmailService {


    private final SendGridService sendGridService;
    private final CommunicationLogRepository logRepository;
    private final String defaultFromEmail;

    public EmailService(SendGridService sendGridService, 
                       CommunicationLogRepository logRepository,
                       @Value("${external.sendgrid.from-email:noreply@eventplanner.com}") String defaultFromEmail) {
        this.sendGridService = sendGridService;
        this.logRepository = logRepository;
        this.defaultFromEmail = defaultFromEmail;
    }

    /**
     * Send email using template (with logging)
     */
    public EmailResponse sendTemplate(String to, String templateId, java.util.Map<String, Object> templateData) {
        return sendTemplate(to, defaultFromEmail, templateId, templateData, true);
    }

    /**
     * Send email using template with custom from address
     */
    public EmailResponse sendTemplate(String to, String from, String templateId, java.util.Map<String, Object> templateData) {
        return sendTemplate(to, from, templateId, templateData, true);
    }

    /**
     * Send email using template with optional logging
     */
    public EmailResponse sendTemplate(String to, String from, String templateId, java.util.Map<String, Object> templateData, boolean logToDatabase) {
        if (!StringUtils.hasText(to)) {
            return EmailResponse.builder()
                    .success(false)
                    .statusMessage("Recipient address is blank")
                    .build();
        }

        if (!StringUtils.hasText(templateId)) {
            return EmailResponse.builder()
                    .success(false)
                    .statusMessage("Template ID is blank")
                    .build();
        }

        
        // Send template email via SendGrid
        EmailResponse response = sendGridService.sendTemplateEmail(to, from, templateId, templateData);
        
        // Log to database if requested
        if (logToDatabase) {
            logEmail(to, from, "Template: " + templateId, "Template email", response);
        }
        
        return response;
    }

    /**
     * Validate email address
     */
    public boolean isValidEmail(String email) {
        return sendGridService.isValidEmail(email);
    }

    /**
     * Log email to database
     */
    private void logEmail(String to, String from, String subject, String content, EmailResponse response) {
        try {
            CommunicationLogEntity entity = new CommunicationLogEntity();
            entity.setChannel("email");
            entity.setRecipient(to);
            entity.setSubject(subject);
            entity.setContent(content);
            entity.setStatus(response.isSuccess() ? CommunicationStatus.SENT : CommunicationStatus.FAILED);
            entity.setMetadata(serializeMetadata(Map.of(
                "from", from,
                "sendGridResponse", response.getStatusMessage(),
                "messageId", response.getMessageId() != null ? response.getMessageId() : ""
            )));
            
            logRepository.save(entity);
        } catch (Exception ex) {
        }
    }

    /**
     * Simple JSON serialization for metadata
     */
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
                builder.append('"').append(sanitize(String.valueOf(value))).append('"');
            });
            builder.append('}');
            return builder.toString();
        } catch (Exception ex) {
            return payload.toString();
        }
    }

    private String sanitize(String input) {
        return input.replace("\"", "\\\"");
    }
}

