package eventplanner.features.event.service;

import eventplanner.features.event.dto.response.CreateEventWithCoverUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service to handle idempotency for event creation operations
 * Ensures event creation is idempotent to prevent duplicates from retries
 */
@Service
@RequiredArgsConstructor
public class EventIdempotencyService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String IDEMPOTENCY_KEY_PREFIX = "event:idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    
    /**
     * Check if an event creation with this idempotency key has already been processed
     */
    public Optional<CreateEventWithCoverUploadResponse> getProcessedResult(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        
        try {
            String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            Object result = redisTemplate.opsForValue().get(key);
            
            if (result instanceof CreateEventWithCoverUploadResponse) {
                return Optional.of((CreateEventWithCoverUploadResponse) result);
            }
        } catch (Exception e) {
            // Redis unavailable, proceed without cache
        }
        
        return Optional.empty();
    }
    
    /**
     * Store the result of an event creation with this idempotency key
     */
    public void storeResult(String idempotencyKey, CreateEventWithCoverUploadResponse result) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        
        try {
            String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            redisTemplate.opsForValue().set(key, result, IDEMPOTENCY_TTL);
        } catch (Exception e) {
            // Redis unavailable, result not cached
        }
    }
    
    /**
     * Check if an operation is already in progress (to prevent concurrent processing)
     */
    public boolean markAsProcessing(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true; // Allow processing if no key
        }
        
        try {
            String lockKey = IDEMPOTENCY_KEY_PREFIX + "lock:" + idempotencyKey;
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "processing", Duration.ofMinutes(5));
            
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            // Redis unavailable, allow processing
            return true;
        }
    }
    
    /**
     * Release the processing lock (called after successful or failed processing)
     */
    public void releaseProcessingLock(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        
        try {
            String lockKey = IDEMPOTENCY_KEY_PREFIX + "lock:" + idempotencyKey;
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            // Redis unavailable, lock will expire naturally
        }
    }
}

