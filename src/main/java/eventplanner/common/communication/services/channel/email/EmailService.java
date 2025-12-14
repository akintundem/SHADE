package eventplanner.common.communication.services.channel.email;

import eventplanner.common.communication.config.ResendConfig;
import eventplanner.common.communication.services.channel.email.dto.EmailResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Email service using Resend API templates
 */
@Service
@Slf4j
public class EmailService {

    public static final String TEMPLATE_EMAIL_VERIFICATION = "email-verification";
    
    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private final ResendConfig resendConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EmailService(ResendConfig resendConfig) {
        this.resendConfig = resendConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send email using a Resend template
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param templateId Resend template ID (e.g., "email-verification")
     * @param variables Map of variables for the template
     * @return EmailResult with success status and message ID
     */
    public EmailResult sendEmail(String to, String subject, String templateId, Map<String, Object> variables) {
        // Check if Resend API key is configured
        if (!resendConfig.isApiKeyConfigured()) {
            log.warn("Resend API key is not configured. Email not sent.");
            return EmailResult.builder()
                    .success(false)
                    .errorMessage("Resend API key is not configured. Please set RESEND_API_KEY environment variable.")
                    .build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendConfig.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", resendConfig.getDefaultFromEmail());
            requestBody.put("to", List.of(to));
            requestBody.put("subject", subject);
            
            Map<String, Object> template = new HashMap<>();
            template.put("id", templateId);
            template.put("variables", variables != null ? variables : new HashMap<>());
            requestBody.put("template", template);

            // Log request details for debugging
            log.info("Sending email via Resend - To: {}, Template ID: {}, From: {}", 
                    to, templateId, resendConfig.getDefaultFromEmail());
            log.debug("Email template variables: {}", variables);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    RESEND_API_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            
            log.debug("Resend API response status: {}, body: {}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String messageId = null;
                if (responseJson.has("id")) {
                    messageId = responseJson.get("id").asText();
                }
                
                log.info("Email sent successfully. Message ID: {}", messageId);
                return EmailResult.builder()
                        .success(true)
                        .messageId(messageId)
                        .build();
            } else {
                String errorMessage = "HTTP " + response.getStatusCode() + ": " + response.getBody();
                log.error("Failed to send email. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return EmailResult.builder()
                        .success(false)
                        .errorMessage(errorMessage)
                        .build();
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Handle HTTP client errors (like domain verification issues)
            String errorMessage = e.getMessage();
            String responseBody = e.getResponseBodyAsString();
            
            log.error("Resend API HTTP error - Status: {}, Response: {}", e.getStatusCode(), responseBody);
            
            if (e.getStatusCode().value() == 403) {
                // Parse the error response to get the actual message
                try {
                    if (responseBody != null) {
                        JsonNode errorJson = objectMapper.readTree(responseBody);
                        if (errorJson.has("message")) {
                            errorMessage = errorJson.get("message").asText();
                        }
                    }
                } catch (Exception parseEx) {
                    log.warn("Failed to parse error response: {}", parseEx.getMessage());
                }
                log.warn("Email sending failed due to domain/configuration issue: {}", errorMessage);
            } else if (e.getStatusCode().value() == 422) {
                // Validation error - template or variables issue
                try {
                    if (responseBody != null) {
                        JsonNode errorJson = objectMapper.readTree(responseBody);
                        if (errorJson.has("message")) {
                            errorMessage = errorJson.get("message").asText();
                        }
                    }
                } catch (Exception parseEx) {
                    log.warn("Failed to parse validation error: {}", parseEx.getMessage());
                }
                log.error("Email validation error: {}", errorMessage);
            } else {
                log.error("Error sending email - HTTP {}: {}", e.getStatusCode(), errorMessage);
            }
            return EmailResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage(), e);
            return EmailResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
