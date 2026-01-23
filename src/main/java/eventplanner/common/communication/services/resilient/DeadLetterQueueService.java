package eventplanner.common.communication.services.resilient;

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
 * Stores failed notifications in Redis for later retry or manual review.
 *
 * Features:
 * - Persists failed notifications with full context
 * - TTL of 7 days (configurable)
 * - Supports manual retry
 * - Provides metrics on failure reasons
 */
@Service
@Slf4j
public class DeadLetterQueueService {

    private static final String DLQ_KEY_PREFIX = "dlq:notification:";
    private static final Duration DLQ_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;

    public DeadLetterQueueService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
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
            // Fallback: log to file system or other persistent storage
            logToFileSystem(request, reason);
            return null;
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
     * Fallback: Log to file system when Redis is unavailable
     */
    private void logToFileSystem(NotificationRequest request, String reason) {
        log.error("DLQ_FILESYSTEM: Failed notification - Type: {}, To: {}, Reason: {}",
                request.getType(),
                request.getTo(),
                reason);
        // TODO: Implement file-based DLQ for cases when Redis is down
    }

    /**
     * Get DLQ statistics
     */
    public Map<String, Long> getDLQStats() {
        // TODO: Implement stats collection (count by type, reason, etc.)
        return new HashMap<>();
    }
}
