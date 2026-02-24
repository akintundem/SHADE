package eventplanner.common.communication.services.channel.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.common.communication.services.channel.email.dto.EmailJobRequest;
import eventplanner.common.communication.services.channel.email.dto.EmailResult;
import eventplanner.common.communication.util.EmailVariableSanitizer;
import eventplanner.common.config.RabbitMqProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Email service publishing notification jobs to RabbitMQ.
 */
@Service
public class EmailService {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final ObjectMapper objectMapper;

    public EmailService(RabbitTemplate rabbitTemplate,
                        RabbitMqProperties rabbitMqProperties,
                        ObjectMapper objectMapper) {
        String exchange = rabbitMqProperties.getExchange();
        String routingKey = rabbitMqProperties.getEmailRoutingKey();
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalStateException("rabbitmq.exchange must be configured");
        }
        if (routingKey == null || routingKey.isBlank()) {
            throw new IllegalStateException("rabbitmq.email-routing-key must be configured");
        }
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Queue an email job for the Node email service to consume.
     *
     * @param to recipient email
     * @param subject email subject (optional; template default will be used if null)
     * @param from sender address (required by the Node service)
     * @param templateId template key/id understood by the Node service
     * @param variables template variables
     */
    public EmailResult sendEmail(String to, String subject, String from, String templateId, Map<String, Object> variables) {
        try {
            // SECURITY: Sanitize template variables to prevent HTML injection in emails.
            // User-controlled values (event names, user names, etc.) are HTML-escaped
            // before being passed to the template renderer.
            Map<String, Object> sanitizedVariables = EmailVariableSanitizer.sanitize(variables);

            EmailJobRequest payload = EmailJobRequest.builder()
                    .templateId(templateId)
                    .to(List.of(to))
                    .from(from)
                    .subject(subject)
                    .variables(sanitizedVariables)
                    .build();

            String payloadJson = objectMapper.writeValueAsString(payload);
            String messageId = UUID.randomUUID().toString();

            rabbitTemplate.convertAndSend(exchange, routingKey, payloadJson, message -> {
                message.getMessageProperties().setContentType(MediaType.APPLICATION_JSON_VALUE);
                message.getMessageProperties().setMessageId(messageId);
                return message;
            });

            return EmailResult.builder()
                    .success(true)
                    .messageId(messageId)
                    .build();
        } catch (Exception e) {
            return EmailResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
