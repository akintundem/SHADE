package eventplanner.features.budget.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service to handle idempotency for budget operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetIdempotencyService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String IDEMPOTENCY_KEY_PREFIX = "budget:idempotency:";
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
}

