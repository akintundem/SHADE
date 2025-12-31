package eventplanner.common.communication.services.channel.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.common.communication.services.channel.email.dto.EmailResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Email service proxying to the local Node email microservice (Resend + React Email).
 */
@Service
@Slf4j
public class EmailService {

    public static final String TEMPLATE_EMAIL_VERIFICATION = "email-verification";

    private final String emailServiceUrl;
    private final String sharedSecret;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EmailService(
            @Value("${external.email-service.url:http://shade-email-service:3000}") String emailServiceUrl,
            @Value("${external.email-service.secret:}") String sharedSecret) {
        this.emailServiceUrl = emailServiceUrl.endsWith("/") ? emailServiceUrl.substring(0, emailServiceUrl.length() - 1) : emailServiceUrl;
        this.sharedSecret = sharedSecret;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send email by POSTing to the Node email service.
     *
     * @param to recipient email
     * @param subject email subject (optional; template default will be used if null)
     * @param from sender address (required by the Node service)
     * @param templateId template key/id understood by the Node service
     * @param variables template variables
     */
    public EmailResult sendEmail(String to, String subject, String from, String templateId, Map<String, Object> variables) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (sharedSecret != null && !sharedSecret.isBlank()) {
                headers.add("x-email-secret", sharedSecret);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("templateId", templateId);
            body.put("to", List.of(to));
            body.put("from", from);
            if (subject != null) {
                body.put("subject", subject);
            }
            body.put("variables", variables != null ? variables : new HashMap<>());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    emailServiceUrl + "/send-email",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                String messageId = extractMessageId(response.getBody());
                log.info("Email sent via Node service. Template: {}, To: {}", templateId, to);
                return EmailResult.builder()
                        .success(true)
                        .messageId(messageId)
                        .build();
            } else {
                String errorMessage = "HTTP " + response.getStatusCode() + ": " + response.getBody();
                log.error("Email service returned error: {}", errorMessage);
                return EmailResult.builder()
                        .success(false)
                        .errorMessage(errorMessage)
                        .build();
            }
        } catch (RestClientException e) {
            log.error("Failed to call email service: {}", e.getMessage(), e);
            return EmailResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage(), e);
            return EmailResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private String extractMessageId(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("id")) {
                return node.get("id").asText();
            }
            if (node.has("data") && node.get("data").has("id")) {
                return node.get("data").get("id").asText();
            }
        } catch (Exception ignored) {
            // best-effort only
        }
        return null;
    }
}
