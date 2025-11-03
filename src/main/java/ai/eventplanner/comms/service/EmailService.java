package ai.eventplanner.comms.service;

import ai.eventplanner.comms.model.CommunicationLogEntity;
import ai.eventplanner.comms.repository.CommunicationLogRepository;
import ai.eventplanner.common.domain.enums.CommunicationStatus;
import ai.eventplanner.resend.dto.EmailResponse;
import ai.eventplanner.resend.service.ResendService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Template email service using Resend with optional logging
 * Focused on sending beautifully designed template emails
 */
@Service
public class EmailService {


    private final ResendService resendService;
    private final CommunicationLogRepository logRepository;
    private final String defaultFromEmail;

    public EmailService(ResendService resendService, 
                       CommunicationLogRepository logRepository,
                       @Value("${external.resend.from-email:noreply@shde.com}") String defaultFromEmail) {
        this.resendService = resendService;
        this.logRepository = logRepository;
        this.defaultFromEmail = defaultFromEmail;
    }

    /**
     * Send HTML email
     */
    public EmailResponse sendEmail(String to, String subject, String html) {
        return sendEmail(to, defaultFromEmail, subject, html, true);
    }

    /**
     * Send HTML email with custom from address
     */
    public EmailResponse sendEmail(String to, String from, String subject, String html) {
        return sendEmail(to, from, subject, html, true);
    }

    /**
     * Send HTML email with optional logging
     */
    public EmailResponse sendEmail(String to, String from, String subject, String html, boolean logToDatabase) {
        if (!StringUtils.hasText(to)) {
            return EmailResponse.builder()
                    .success(false)
                    .statusMessage("Recipient address is blank")
                    .build();
        }

        if (!StringUtils.hasText(subject)) {
            return EmailResponse.builder()
                    .success(false)
                    .statusMessage("Subject is blank")
                    .build();
        }

        if (!StringUtils.hasText(html)) {
            return EmailResponse.builder()
                    .success(false)
                    .statusMessage("Email content is blank")
                    .build();
        }

        // Send email via Resend
        EmailResponse response = resendService.sendEmail(to, from, subject, html);
        
        // Log to database if requested
        if (logToDatabase) {
            logEmail(to, from, subject, "HTML email", response);
        }
        
        return response;
    }

    /**
     * Validate email address
     */
    public boolean isValidEmail(String email) {
        return resendService.isValidEmail(email);
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
                "resendResponse", response.getStatusMessage(),
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

