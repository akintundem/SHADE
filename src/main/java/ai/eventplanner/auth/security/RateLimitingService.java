package ai.eventplanner.auth.security;

import ai.eventplanner.auth.entity.ClientApplication;
import ai.eventplanner.auth.service.ClientValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Service for implementing rate limiting per client
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ClientValidationService clientValidationService;

    private static final String RATE_LIMIT_MINUTE_PREFIX = "rate_limit_minute:";
    private static final String RATE_LIMIT_HOUR_PREFIX = "rate_limit_hour:";

    /**
     * Check if client is within rate limits
     */
    public boolean isWithinRateLimit(String clientId, String endpoint) {
        try {
            ClientApplication client = clientValidationService.getClientById(clientId).orElse(null);
            if (client == null) {
                return false;
            }

            String minuteKey = RATE_LIMIT_MINUTE_PREFIX + clientId + ":" + endpoint + ":" + getCurrentMinute();
            String hourKey = RATE_LIMIT_HOUR_PREFIX + clientId + ":" + endpoint + ":" + getCurrentHour();

            // Check minute rate limit
            String minuteCount = redisTemplate.opsForValue().get(minuteKey);
            int currentMinuteCount = minuteCount != null ? Integer.parseInt(minuteCount) : 0;
            
            if (currentMinuteCount >= client.getRateLimitPerMinute()) {
                log.warn("Rate limit exceeded for client {} on endpoint {} (minute limit: {})", 
                        clientId, endpoint, client.getRateLimitPerMinute());
                return false;
            }

            // Check hour rate limit
            String hourCount = redisTemplate.opsForValue().get(hourKey);
            int currentHourCount = hourCount != null ? Integer.parseInt(hourCount) : 0;
            
            if (currentHourCount >= client.getRateLimitPerHour()) {
                log.warn("Rate limit exceeded for client {} on endpoint {} (hour limit: {})", 
                        clientId, endpoint, client.getRateLimitPerHour());
                return false;
            }

            // Increment counters
            redisTemplate.opsForValue().increment(minuteKey);
            redisTemplate.expire(minuteKey, Duration.ofMinutes(1));
            
            redisTemplate.opsForValue().increment(hourKey);
            redisTemplate.expire(hourKey, Duration.ofHours(1));

            return true;

        } catch (Exception e) {
            log.error("Error checking rate limit for client {}: {}", clientId, e.getMessage());
            // In case of error, allow the request but log it
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
     * Get remaining rate limit for client
     */
    public RateLimitInfo getRateLimitInfo(String clientId, String endpoint) {
        try {
            ClientApplication client = clientValidationService.getClientById(clientId).orElse(null);
            if (client == null) {
                return new RateLimitInfo(0, 0, 0, 0);
            }

            String minuteKey = RATE_LIMIT_MINUTE_PREFIX + clientId + ":" + endpoint + ":" + getCurrentMinute();
            String hourKey = RATE_LIMIT_HOUR_PREFIX + clientId + ":" + endpoint + ":" + getCurrentHour();

            String minuteCount = redisTemplate.opsForValue().get(minuteKey);
            String hourCount = redisTemplate.opsForValue().get(hourKey);

            int currentMinuteCount = minuteCount != null ? Integer.parseInt(minuteCount) : 0;
            int currentHourCount = hourCount != null ? Integer.parseInt(hourCount) : 0;

            return new RateLimitInfo(
                    currentMinuteCount,
                    client.getRateLimitPerMinute(),
                    currentHourCount,
                    client.getRateLimitPerHour()
            );

        } catch (Exception e) {
            log.error("Error getting rate limit info for client {}: {}", clientId, e.getMessage());
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
