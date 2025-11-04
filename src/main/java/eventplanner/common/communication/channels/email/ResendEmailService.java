package eventplanner.common.communication.channels.email;

import eventplanner.common.communication.config.ResendConfig;
import eventplanner.common.communication.channels.email.dto.EmailRequest;
import eventplanner.common.communication.channels.email.dto.EmailResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resend email service implementation
 */
@Service
public class ResendEmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private final ResendConfig resendConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ResendEmailService(ResendConfig resendConfig) {
        this.resendConfig = resendConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send a simple email
     */
    public EmailResponse sendEmail(String to, String from, String subject, String html) {
        EmailRequest request = EmailRequest.builder()
                .to(List.of(to))
                .from(from)
                .subject(subject)
                .html(html)
                .build();

        return sendEmail(request);
    }

    /**
     * Send email using EmailRequest
     */
    public EmailResponse sendEmail(EmailRequest emailRequest) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendConfig.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", emailRequest.getFrom());
            requestBody.put("to", emailRequest.getTo());
            requestBody.put("subject", emailRequest.getSubject());
            
            if (emailRequest.getHtml() != null) {
                requestBody.put("html", emailRequest.getHtml());
            }
            if (emailRequest.getText() != null) {
                requestBody.put("text", emailRequest.getText());
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
}

