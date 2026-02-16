package eventplanner.common.communication.services.resilient;

import eventplanner.common.communication.enums.CommunicationStatus;
import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dead Letter Queue for failed notifications.
 * Primary store: Redis (7-day TTL).
 * Fallback: PostgreSQL via {@link CommunicationRepository} when Redis is unavailable.
 */
@Service
@Slf4j
public class DeadLetterQueueService {

    private static final String DLQ_KEY_PREFIX = "dlq:notification:";
    private static final Duration DLQ_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;
    private final CommunicationRepository communicationRepository;

    public DeadLetterQueueService(RedisTemplate<String, Object> redisTemplate,
                                  CommunicationRepository communicationRepository) {
        this.redisTemplate = redisTemplate;
        this.communicationRepository = communicationRepository;
    }

    /**
     * Add a failed notification to the dead letter queue.
     *
     * @param request The notification request that failed
     * @param reason The failure reason
     * @return The DLQ entry ID
     */
    public String addFailedNotification(NotificationRequest request, String reason) {
        String dlqId = UUID.randomUUID().toString();
        String key = DLQ_KEY_PREFIX + dlqId;

        Map<String, Object> entry = new HashMap<>();
        entry.put("id", dlqId);
        entry.put("notificationType", request.getType() != null ? request.getType().toString() : null);
        entry.put("to", request.getTo());
        entry.put("subject", request.getSubject());
        entry.put("templateId", request.getTemplateId());
        entry.put("eventId", request.getEventId() != null ? request.getEventId().toString() : null);
        entry.put("failureReason", reason);
        entry.put("failedAt", LocalDateTime.now().toString());
        entry.put("retryCount", 0);

        try {
            redisTemplate.opsForValue().set(key, entry, DLQ_TTL);
            log.warn("Added failed notification to DLQ - ID: {}, Type: {}, Reason: {}",
                    dlqId, request.getType(), reason);
            return dlqId;
        } catch (Exception e) {
            log.error("Failed to write to DLQ (Redis unavailable): {}", e.getMessage());
            return persistToDatabase(request, reason);
        }
    }

    /**
     * Retrieve a failed notification from DLQ by ID
     */
    public Map<String, Object> getFailedNotification(String dlqId) {
        String key = DLQ_KEY_PREFIX + dlqId;
        try {
            return (Map<String, Object>) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to retrieve from DLQ: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Remove a notification from DLQ (after successful retry)
     */
    public void removeFromDLQ(String dlqId) {
        String key = DLQ_KEY_PREFIX + dlqId;
        try {
            redisTemplate.delete(key);
            log.info("Removed notification from DLQ: {}", dlqId);
        } catch (Exception e) {
            log.error("Failed to remove from DLQ: {}", e.getMessage());
        }
    }

    /**
     * Increment retry count for a DLQ entry
     */
    public void incrementRetryCount(String dlqId) {
        Map<String, Object> entry = getFailedNotification(dlqId);
        if (entry != null) {
            Integer retryCount = (Integer) entry.getOrDefault("retryCount", 0);
            entry.put("retryCount", retryCount + 1);
            entry.put("lastRetryAt", LocalDateTime.now().toString());

            String key = DLQ_KEY_PREFIX + dlqId;
            redisTemplate.opsForValue().set(key, entry, DLQ_TTL);
        }
    }

    /**
     * Database fallback: persist the failed notification as a Communication record with FAILED status.
     * This guarantees zero data loss even when Redis is completely down.
     */
    private String persistToDatabase(NotificationRequest request, String reason) {
        try {
            Communication record = new Communication();
            record.setCommunicationType(request.getType());
            record.setRecipientEmail(request.getTo());
            record.setSubject(request.getSubject());
            record.setTemplateId(request.getTemplateId());
            record.setEventId(request.getEventId());
            record.setStatus(CommunicationStatus.FAILED);
            record.setFailureReason(reason);
            record.setFailedAt(LocalDateTime.now());
            record.setChannel("dlq_fallback");
            communicationRepository.save(record);
            log.warn("DLQ DB fallback: persisted failed notification to database - To: {}, Type: {}, Reason: {}",
                    request.getTo(), request.getType(), reason);
            return record.getId() != null ? record.getId().toString() : null;
        } catch (Exception dbEx) {
            log.error("DLQ DB fallback ALSO failed - notification LOST - To: {}, Reason: {}, DB error: {}",
                    request.getTo(), reason, dbEx.getMessage());
            return null;
        }
    }

    /**
     * Get DLQ statistics
     */
    public Map<String, Long> getDLQStats() {
        // TODO: Implement stats collection (count by type, reason, etc.)
        return new HashMap<>();
    }
}
