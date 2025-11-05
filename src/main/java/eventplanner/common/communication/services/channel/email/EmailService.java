package eventplanner.common.communication.services.channel.email;

import eventplanner.common.communication.config.ResendConfig;
import eventplanner.common.communication.services.channel.email.dto.EmailRequest;
import eventplanner.common.communication.services.channel.email.dto.EmailResponse;
import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.domain.enums.CommunicationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Email service using Resend API with logging
 */
@Service
@Slf4j
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private final ResendConfig resendConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CommunicationRepository communicationRepository;
    private final String defaultFromEmail;

    public EmailService(ResendConfig resendConfig,
                       CommunicationRepository communicationRepository,
                       @Value("${external.resend.from-email:noreply@shde.com}") String defaultFromEmail) {
        this.resendConfig = resendConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.communicationRepository = communicationRepository;
        this.defaultFromEmail = defaultFromEmail;
    }

    /**
     * Send a simple email with default from address
     */
    public EmailResponse sendEmail(String to, String subject, String html) {
        return sendEmail(to, defaultFromEmail, subject, html, null, true);
    }

    /**
     * Send a simple email with custom from address
     */
    public EmailResponse sendEmail(String to, String from, String subject, String html) {
        return sendEmail(to, from, subject, html, null, true);
    }

    /**
     * Send email with optional event context and logging
     */
    public EmailResponse sendEmail(String to, String from, String subject, String html, UUID eventId, boolean logToDatabase) {
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

        EmailRequest request = EmailRequest.builder()
                .to(List.of(to))
                .from(from)
                .subject(subject)
                .html(html)
                .build();

        EmailResponse response = sendEmailRequest(request);
        
        // Log to database if requested
        if (logToDatabase) {
            logEmail(to, from, subject, html, response, eventId);
        }
        
        return response;
    }
    
    /**
     * Send email using EmailRequest (for advanced use cases)
     */
    public EmailResponse sendEmail(EmailRequest emailRequest) {
        return sendEmailRequest(emailRequest);
    }

    /**
     * Send email using a Resend template
     * 
     * @param to Recipient email address
     * @param from Sender email address
     * @param subject Email subject
     * @param templateId Resend template ID (e.g., "email-verification")
     * @param templateVariables Map of variables to pass to the template
     * @param eventId Optional event ID for logging
     * @param logToDatabase Whether to log the email to database
     * @return EmailResponse with send status
     */
    public EmailResponse sendEmailWithTemplate(String to, String from, String subject, 
                                                String templateId, Map<String, Object> templateVariables,
                                                UUID eventId, boolean logToDatabase) {
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

        if (!StringUtils.hasText(templateId)) {
            return EmailResponse.builder()
                    .success(false)
                    .statusMessage("Template ID is blank")
                    .build();
        }

        EmailRequest request = EmailRequest.builder()
                .to(List.of(to))
                .from(from)
                .subject(subject)
                .templateId(templateId)
                .templateVariables(templateVariables != null ? templateVariables : new HashMap<>())
                .build();

        EmailResponse response = sendEmailRequest(request);
        
        // Log to database if requested
        if (logToDatabase) {
            String contentPreview = "Template: " + templateId + " with variables: " + 
                    (templateVariables != null ? templateVariables.toString() : "none");
            logEmail(to, from, subject, contentPreview, response, eventId);
        }
        
        return response;
    }

    /**
     * Send email using a Resend template with default from address
     */
    public EmailResponse sendEmailWithTemplate(String to, String subject, String templateId, 
                                                Map<String, Object> templateVariables) {
        return sendEmailWithTemplate(to, defaultFromEmail, subject, templateId, templateVariables, null, true);
    }
    
    private EmailResponse sendEmailRequest(EmailRequest emailRequest) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendConfig.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", emailRequest.getFrom());
            requestBody.put("to", emailRequest.getTo());
            requestBody.put("subject", emailRequest.getSubject());
            
            // Use template if provided, otherwise use html/text
            if (emailRequest.getTemplateId() != null && !emailRequest.getTemplateId().trim().isEmpty()) {
                // Use Resend template
                Map<String, Object> template = new HashMap<>();
                template.put("id", emailRequest.getTemplateId());
                if (emailRequest.getTemplateVariables() != null && !emailRequest.getTemplateVariables().isEmpty()) {
                    template.put("variables", emailRequest.getTemplateVariables());
                }
                requestBody.put("template", template);
            } else {
                // Use traditional html/text content
                if (emailRequest.getHtml() != null) {
                    requestBody.put("html", emailRequest.getHtml());
                }
                if (emailRequest.getText() != null) {
                    requestBody.put("text", emailRequest.getText());
                }
            }

            if (emailRequest.getAttachments() != null && !emailRequest.getAttachments().isEmpty()) {
                List<Map<String, String>> attachments = emailRequest.getAttachments().stream()
                        .map(att -> {
                            Map<String, String> attMap = new HashMap<>();
                            attMap.put("content", att.getContent());
                            attMap.put("filename", att.getFilename());
                            if (att.getType() != null) {
                                attMap.put("type", att.getType());
                            }
                            return attMap;
                        })
                        .toList();
                requestBody.put("attachments", attachments);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    RESEND_API_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            // Parse response
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            
            String messageId = null;
            if (responseJson.has("id")) {
                messageId = responseJson.get("id").asText();
            }

            return EmailResponse.builder()
                    .success(response.getStatusCode().is2xxSuccessful())
                    .messageId(messageId)
                    .statusCode(response.getStatusCode().value())
                    .statusMessage(response.getBody())
                    .sentAt(LocalDateTime.now())
                    .build();

        } catch (RestClientException e) {
            return EmailResponse.builder()
                    .success(false)
                    .statusCode(500)
                    .statusMessage("Failed to send email: " + e.getMessage())
                    .errors(List.of(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return EmailResponse.builder()
                    .success(false)
                    .statusCode(500)
                    .statusMessage("Failed to send email: " + e.getMessage())
                    .errors(List.of(e.getMessage()))
                    .build();
        }
    }

    /**
     * Validate email address
     */
    public boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }
    
    /**
     * Log email to database
     */
    private void logEmail(String to, String from, String subject, String content, EmailResponse response, UUID eventId) {
        try {
            Communication communication = new Communication();
            if (eventId != null) {
                communication.setEventId(eventId);
            }
            communication.setChannel("email");
            communication.setRecipientEmail(to);
            communication.setSubject(subject);
            communication.setContent(content);
            communication.setStatus(response.isSuccess() ? CommunicationStatus.SENT : CommunicationStatus.FAILED);
            if (response.isSuccess() && response.getSentAt() != null) {
                communication.setSentAt(response.getSentAt());
            }
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("from", from);
            metadata.put("messageId", response.getMessageId() != null ? response.getMessageId() : "");
            metadata.put("statusMessage", response.getStatusMessage());
            communication.setMetadata(serializeMetadata(metadata));
            
            communicationRepository.save(communication);
        } catch (Exception ex) {
            log.warn("Failed to log email communication", ex);
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

