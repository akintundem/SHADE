package ai.eventplanner.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Service for implementing rate limiting per user (authenticated) or IP (unauthenticated)
 * This provides proper protection against abuse by individual users
 */
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String RATE_LIMIT_MINUTE_PREFIX = "rate_limit_minute:";
    private static final String RATE_LIMIT_HOUR_PREFIX = "rate_limit_hour:";

    /**
     * Check if rate limit key (user or IP) is within rate limits
     */
    public boolean isWithinRateLimit(String rateLimitKey, String endpoint) {
        try {
            // Determine rate limits based on key type
            int minuteLimit, hourLimit;
            
            if (rateLimitKey.startsWith("user:")) {
                // User-based rate limits (higher for authenticated users)
                minuteLimit = 100;
                hourLimit = 1000;
            } else if (rateLimitKey.startsWith("ip:")) {
                // IP-based rate limits (lower for unauthenticated requests)
                minuteLimit = 20;
                hourLimit = 200;
            } else {
                // Fallback for unknown key types
                minuteLimit = 10;
                hourLimit = 100;
            }
            
            // Apply stricter limits for authentication endpoints
            if (endpoint.contains("/auth/login") || endpoint.contains("/auth/register")) {
                minuteLimit = Math.min(minuteLimit, 100);
                hourLimit = Math.min(hourLimit, 1000);
            }

            String minuteKey = RATE_LIMIT_MINUTE_PREFIX + rateLimitKey + ":" + endpoint + ":" + getCurrentMinute();
            String hourKey = RATE_LIMIT_HOUR_PREFIX + rateLimitKey + ":" + endpoint + ":" + getCurrentHour();

            // Check minute rate limit
            String minuteCount = redisTemplate.opsForValue().get(minuteKey);
            int currentMinuteCount = minuteCount != null ? Integer.parseInt(minuteCount) : 0;
            
            if (currentMinuteCount >= minuteLimit) {
                return false;
            }

            // Check hour rate limit
            String hourCount = redisTemplate.opsForValue().get(hourKey);
            int currentHourCount = hourCount != null ? Integer.parseInt(hourCount) : 0;
            
            if (currentHourCount >= hourLimit) {
                return false;
            }

            // Increment counters atomically
            redisTemplate.opsForValue().increment(minuteKey);
            redisTemplate.expire(minuteKey, Duration.ofMinutes(1));
            
            redisTemplate.opsForValue().increment(hourKey);
            redisTemplate.expire(hourKey, Duration.ofHours(1));

            return true;

        } catch (Exception e) {
            // On Redis error: default-deny for sensitive auth endpoints, allow for others
            if (endpoint.contains("/auth/login") || endpoint.contains("/auth/register")) {
                return false;
            }
            return true;
        }
    }

    /**
     * Get current minute timestamp for rate limiting
     */
    private String getCurrentMinute() {
        return String.valueOf(Instant.now().getEpochSecond() / 60);
    }

    /**
     * Get current hour timestamp for rate limiting
     */
    private String getCurrentHour() {
        return String.valueOf(Instant.now().getEpochSecond() / 3600);
    }
    

    /**
     * Get remaining rate limit for rate limit key (user or IP)
     */
    public RateLimitInfo getRateLimitInfo(String rateLimitKey, String endpoint) {
        try {
            // Determine rate limits based on key type
            int minuteLimit, hourLimit;
            
            if (rateLimitKey.startsWith("user:")) {
                // User-based rate limits (higher for authenticated users)
                minuteLimit = 100;
                hourLimit = 1000;
            } else if (rateLimitKey.startsWith("ip:")) {
                // IP-based rate limits (lower for unauthenticated requests)
                minuteLimit = 20;
                hourLimit = 200;
            } else {
                // Fallback for unknown key types
                minuteLimit = 10;
                hourLimit = 100;
            }
            
            // Apply stricter limits for authentication endpoints
            if (endpoint.contains("/auth/login") || endpoint.contains("/auth/register")) {
                minuteLimit = Math.min(minuteLimit, 100);
                hourLimit = Math.min(hourLimit, 1000);
            }

            String minuteKey = RATE_LIMIT_MINUTE_PREFIX + rateLimitKey + ":" + endpoint + ":" + getCurrentMinute();
            String hourKey = RATE_LIMIT_HOUR_PREFIX + rateLimitKey + ":" + endpoint + ":" + getCurrentHour();

            String minuteCount = redisTemplate.opsForValue().get(minuteKey);
            String hourCount = redisTemplate.opsForValue().get(hourKey);

            int currentMinuteCount = minuteCount != null ? Integer.parseInt(minuteCount) : 0;
            int currentHourCount = hourCount != null ? Integer.parseInt(hourCount) : 0;

            return new RateLimitInfo(
                    currentMinuteCount,
                    minuteLimit,
                    currentHourCount,
                    hourLimit
            );

        } catch (Exception e) {
            return new RateLimitInfo(0, 0, 0, 0);
        }
    }

    public static class RateLimitInfo {
        public final int currentMinute;
        public final int limitMinute;
        public final int currentHour;
        public final int limitHour;

        public RateLimitInfo(int currentMinute, int limitMinute, int currentHour, int limitHour) {
            this.currentMinute = currentMinute;
            this.limitMinute = limitMinute;
            this.currentHour = currentHour;
            this.limitHour = limitHour;
        }
    }
}
