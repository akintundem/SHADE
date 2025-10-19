package ai.eventplanner.comms.service;

import ai.eventplanner.comms.model.CommunicationLogEntity;
import ai.eventplanner.comms.repository.CommunicationLogRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

@Service
public class CommsService {

    private static final Logger logger = LoggerFactory.getLogger(CommsService.class);

    private final CommunicationLogRepository logRepository;
    private final JavaMailSender mailSender;

    public CommsService(CommunicationLogRepository logRepository, ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.logRepository = logRepository;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    public CommunicationLogEntity queueEmail(Map<String, Object> payload) {
        String to = payload.getOrDefault("to", "").toString();
        String subject = payload.getOrDefault("subject", "Event Update").toString();
        String body = payload.getOrDefault("body", "").toString();

        CommunicationLogEntity entity = new CommunicationLogEntity();
        entity.setChannel("email");
        entity.setRecipient(to);
        entity.setSubject(subject);
        entity.setContent(body);
        entity.setStatus("queued");
        entity.setMetadata(serializeMetadata(payload));

        CommunicationLogEntity saved = logRepository.save(entity);
        attemptMailSend(saved);
        return saved;
    }

    public CommunicationLogEntity queueSms(Map<String, Object> payload) {
        String to = payload.getOrDefault("to", "").toString();
        String body = payload.getOrDefault("message", "").toString();

        CommunicationLogEntity entity = new CommunicationLogEntity();
        entity.setChannel("sms");
        entity.setRecipient(to);
        entity.setContent(body);
        entity.setStatus("queued");
        entity.setMetadata(serializeMetadata(payload));

        return logRepository.save(entity);
    }

    private void attemptMailSend(CommunicationLogEntity entity) {
        if (mailSender == null) {
            logger.info("No JavaMailSender configured; leaving communication {} in queued state.", entity.getId());
            return;
        }
        if (!StringUtils.hasText(entity.getRecipient())) {
            logger.warn("Skipping email send for communication {} due to missing recipient.", entity.getId());
            return;
        }
        try {
            var message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(entity.getRecipient());
            message.setSubject(StringUtils.hasText(entity.getSubject()) ? entity.getSubject() : "Event Planner Update");
            message.setText(entity.getContent() == null ? "" : entity.getContent());
            mailSender.send(message);
            entity.setStatus("sent");
            logRepository.save(entity);
        } catch (MailException ex) {
            logger.info("Mail send skipped for communication {} ({}). cause={}",
                    entity.getId(), entity.getRecipient(), ex.getMessage());
            entity.setStatus("queued");
            logRepository.save(entity);
        }
    }

    private String serializeMetadata(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            // Do a very small manual JSON serialization to avoid adding dependencies.
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
            logger.warn("Failed to serialize communication metadata, storing fallback text. cause={}", ex.getMessage());
            return payload.toString();
        }
    }

    private String sanitize(String input) {
        return input.replace("\"", "\\\"");
    }
}
