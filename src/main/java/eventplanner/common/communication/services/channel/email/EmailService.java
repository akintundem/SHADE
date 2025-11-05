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

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    RESEND_API_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

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

        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage(), e);
            return EmailResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
