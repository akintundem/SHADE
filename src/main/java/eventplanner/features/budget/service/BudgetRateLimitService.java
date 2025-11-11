package eventplanner.features.budget.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service to handle rate limiting for budget bulk operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetRateLimitService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String RATE_LIMIT_KEY_PREFIX = "budget:ratelimit:";
    
    // Rate limit: 10 bulk operations per minute per user
    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    
    // Maximum items per bulk request
    private static final int MAX_BULK_ITEMS = 100;
    
    /**
     * Check if the user has exceeded rate limits for bulk operations
     * @param userId User ID
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String userId) {
        if (userId == null) {
            return false;
        }
        
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == null) {
            return false;
        }
        
        // Set expiry on first request
        if (count == 1) {
            redisTemplate.expire(key, WINDOW.getSeconds(), TimeUnit.SECONDS);
        }
        
        boolean allowed = count <= MAX_REQUESTS;
        
        if (!allowed) {
            log.warn("Rate limit exceeded for user: {}. Count: {}", userId, count);
        }
        
        return allowed;
    }
    
    /**
     * Validate the size of a bulk request
     * @param itemCount Number of items in the bulk request
     * @return true if within limits, false otherwise
     */
    public boolean validateBulkSize(int itemCount) {
        return itemCount > 0 && itemCount <= MAX_BULK_ITEMS;
    }
    
    /**
     * Get the maximum allowed bulk size
     */
    public int getMaxBulkSize() {
        return MAX_BULK_ITEMS;
    }
    
    /**
     * Get remaining requests for a user
     */
    public int getRemainingRequests(String userId) {
        if (userId == null) {
            return 0;
        }
        
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        Long count = Long.parseLong(redisTemplate.opsForValue().get(key) != null 
            ? redisTemplate.opsForValue().get(key) 
            : "0");
        
        return Math.max(0, MAX_REQUESTS - count.intValue());
    }
}

