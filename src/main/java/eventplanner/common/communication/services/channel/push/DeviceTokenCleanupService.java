package eventplanner.common.communication.services.channel.push;

import eventplanner.common.communication.model.DeviceToken;
import eventplanner.common.communication.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for cleaning up invalid and stale device tokens
 * Implements industry-standard token maintenance practices
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceTokenCleanupService {

    private final DeviceTokenRepository deviceTokenRepository;
    
    // Configuration
    private static final int MAX_FAILURES_BEFORE_INVALIDATION = 3;
    private static final int DAYS_BEFORE_STALE_TOKEN_CLEANUP = 90; // 90 days of inactivity
    private static final int DAYS_BEFORE_INVALID_TOKEN_DELETION = 30; // Delete invalid tokens after 30 days

    /**
     * Clean up invalid and stale tokens
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupInvalidTokens() {
        log.info("Starting device token cleanup job");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(DAYS_BEFORE_INVALID_TOKEN_DELETION);
        
        // Find tokens that have been invalidated for more than 30 days
        List<DeviceToken> invalidTokens = deviceTokenRepository.findByIsActiveFalse();
        int deletedCount = 0;
        
        for (DeviceToken token : invalidTokens) {
            if (token.getInvalidatedAt() != null && token.getInvalidatedAt().isBefore(cutoffDate)) {
                deviceTokenRepository.delete(token);
                deletedCount++;
            }
        }
        
        log.info("Deleted {} invalid device tokens older than {} days", deletedCount, DAYS_BEFORE_INVALID_TOKEN_DELETION);
    }

    /**
     * Deactivate stale tokens that haven't been used recently
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    @Transactional
    public void deactivateStaleTokens() {
        log.info("Starting stale device token deactivation job");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(DAYS_BEFORE_STALE_TOKEN_CLEANUP);
        
        int deactivatedCount = deviceTokenRepository.deactivateOldTokens(cutoffDate);
        
        log.info("Deactivated {} stale device tokens (not used in {} days)", 
                deactivatedCount, DAYS_BEFORE_STALE_TOKEN_CLEANUP);
    }

    /**
     * Invalidate tokens with excessive failure counts
     * Runs every 6 hours
     */
    @Scheduled(cron = "0 0 */6 * * *") // Every 6 hours
    @Transactional
    public void invalidateFailedTokens() {
        log.info("Starting failed token invalidation job");
        
        List<DeviceToken> activeTokens = deviceTokenRepository.findByIsActiveTrue();
        int invalidatedCount = 0;
        
        for (DeviceToken token : activeTokens) {
            if (token.shouldBeInvalidated(MAX_FAILURES_BEFORE_INVALIDATION)) {
                token.markAsInvalid("Exceeded max failure count: " + token.getFailureCount());
                deviceTokenRepository.save(token);
                invalidatedCount++;
            }
        }
        
        log.info("Invalidated {} device tokens with excessive failure counts", invalidatedCount);
    }
}

