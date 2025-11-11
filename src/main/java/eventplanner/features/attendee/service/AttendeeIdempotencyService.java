package eventplanner.features.attendee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service to handle idempotency for attendee operations
 * Ensures invitations and check-ins are idempotent to prevent duplicates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttendeeIdempotencyService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String IDEMPOTENCY_KEY_PREFIX = "attendee:idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    
    /**
     * Check if an operation with this idempotency key has already been processed
     */
    public Optional<String> getProcessedResult(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        String result = redisTemplate.opsForValue().get(key);
        
        if (result != null) {
            log.info("Found cached idempotent result for key: {}", idempotencyKey);
        }
        
        return Optional.ofNullable(result);
    }
    
    /**
     * Store the result of an operation with this idempotency key
     */
    public void storeResult(String idempotencyKey, String result) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, result, IDEMPOTENCY_TTL);
        log.info("Stored idempotent result for key: {}", idempotencyKey);
    }
    
    /**
     * Check if an operation is already in progress (to prevent concurrent processing)
     */
    public boolean markAsProcessing(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true; // Allow processing if no key
        }
        
        String lockKey = IDEMPOTENCY_KEY_PREFIX + "lock:" + idempotencyKey;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "processing", Duration.ofMinutes(5));
        
        if (Boolean.TRUE.equals(success)) {
            log.debug("Marked operation as processing for key: {}", idempotencyKey);
            return true;
        } else {
            log.warn("Operation already in progress for key: {}", idempotencyKey);
            return false;
        }
    }
    
    /**
     * Release the processing lock (called after successful or failed processing)
     */
    public void releaseProcessingLock(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        
        String lockKey = IDEMPOTENCY_KEY_PREFIX + "lock:" + idempotencyKey;
        redisTemplate.delete(lockKey);
        log.debug("Released processing lock for key: {}", idempotencyKey);
    }
}

