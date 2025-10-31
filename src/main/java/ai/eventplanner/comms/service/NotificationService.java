package ai.eventplanner.comms.service;

import ai.eventplanner.comms.entity.Communication;
import ai.eventplanner.comms.repo.CommunicationRepository;
import ai.eventplanner.common.domain.enums.CommunicationStatus;
import ai.eventplanner.common.domain.enums.CommunicationType;
import ai.eventplanner.common.domain.enums.RecipientType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final CommunicationRepository communicationRepository;

    public Communication send(UUID eventId, CommunicationType channel, String subject, String content,
                              List<String> recipientEmails, LocalDateTime scheduledAt, String priority) {
        String idempotencyKey = buildIdempotencyKey(eventId, subject, content, recipientEmails);
        // In a real provider integration, we would check a cache/store for idempotencyKey
        log.info("notification_send request eventId={} channel={} scheduledAt={} priority={} idemKey={}",
                eventId, channel, scheduledAt, priority, idempotencyKey);
        Communication c = new Communication();
        c.setEventId(eventId);
        c.setCommunicationType(channel);
        c.setRecipientType(RecipientType.USER);
        c.setSubject(subject);
        c.setContent(content);
        c.setScheduledAt(scheduledAt);
        c.setStatus(CommunicationStatus.PENDING);
        // naive persistence of the first recipient email for tracking; bulk handling can be added
        if (recipientEmails != null && !recipientEmails.isEmpty()) {
            c.setRecipientEmail(recipientEmails.get(0));
        }
        Communication saved = communicationRepository.save(c);
        // In a real integration, enqueue delivery via provider; here we mark sent immediately if not scheduled
        if (scheduledAt == null) {
            saved.setStatus(CommunicationStatus.SENT);
            saved.setSentAt(LocalDateTime.now());
            log.info("notification_sent eventId={} communicationId={}", eventId, saved.getId());
        }
        return saved;
    }

    private String buildIdempotencyKey(UUID eventId, String subject, String content, List<String> emails) {
        String base = eventId + "|" + (subject != null ? subject : "") + "|" + (content != null ? Integer.toString(content.hashCode()) : "") + "|" + (emails != null ? emails.toString() : "");
        return Integer.toHexString(base.hashCode());
    }
}

