package eventplanner.features.feeds.service;

import eventplanner.common.storage.s3.services.S3StorageService;
import eventplanner.common.storage.upload.MediaUploadStatus;
import eventplanner.features.feeds.entity.EventFeedPost;
import eventplanner.features.feeds.repository.FeedPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled service to clean up incomplete feed post uploads.
 * Removes posts that have been in PENDING status for too long (likely failed uploads).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedPostCleanupService {

    private static final String EVENT_BUCKET_ALIAS = "event";
    private static final String PURPOSE_POST_MEDIA = "post_media";
    
    private final FeedPostRepository postRepository;
    private final S3StorageService storageService;
    
    /**
     * Maximum age for incomplete uploads before cleanup (default: 5 minutes)
     * Can be configured via application.yml: feeds.cleanup.max-age-minutes
     */
    @Value("${feeds.cleanup.max-age-minutes:5}")
    private int maxAgeMinutes;

    /**
     * Scheduled task that runs every 5 minutes to clean up incomplete uploads.
     * Cron expression: second, minute, hour, day, month, weekday
     * "0 * /5 * * * *" means: at 0 seconds of every 5th minute
     * 
     * Alternative: Run every minute with "0 * * * * *"
     */
    @Scheduled(cron = "${feeds.cleanup.cron:0 */5 * * * *}")
    @Transactional
    public void cleanupIncompleteUploads() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minus(maxAgeMinutes, ChronoUnit.MINUTES);
            List<EventFeedPost> incomplete = postRepository.findByMediaUploadStatusAndCreatedAtBefore(
                    MediaUploadStatus.PENDING,
                    cutoff
            );

            if (incomplete.isEmpty()) {
                log.debug("No incomplete feed posts to clean up");
                return;
            }

            log.info("Cleaning up {} incomplete feed posts older than {} minutes", incomplete.size(), maxAgeMinutes);

            int deletedCount = 0;
            int s3CleanupCount = 0;
            int s3CleanupFailures = 0;

            for (EventFeedPost post : incomplete) {
                try {
                    // Delete orphaned S3 object if it exists
                    if (post.getMediaObjectId() != null) {
                        try {
                            String objectKey = buildObjectKey(post.getEventId(), post.getMediaObjectId());
                            storageService.deleteObject(EVENT_BUCKET_ALIAS, objectKey);
                            s3CleanupCount++;
                            log.debug("Deleted S3 object {} for incomplete post {}", objectKey, post.getId());
                        } catch (Exception e) {
                            s3CleanupFailures++;
                            log.warn("Failed to delete S3 object for post {}: {}", post.getId(), e.getMessage());
                            // Continue with post deletion even if S3 cleanup fails
                        }
                    }
                    
                    // Delete the incomplete post
                    postRepository.delete(post);
                    deletedCount++;
                    
                } catch (Exception e) {
                    log.error("Failed to cleanup incomplete post {}: {}", post.getId(), e.getMessage(), e);
                }
            }

            log.info("Cleanup complete: {} posts deleted, {} S3 objects cleaned, {} S3 cleanup failures", 
                    deletedCount, s3CleanupCount, s3CleanupFailures);
                    
        } catch (Exception e) {
            log.error("Error in scheduled feed post cleanup", e);
        }
    }

    private String buildObjectKey(UUID eventId, UUID mediaId) {
        return "events/" + eventId + "/" + PURPOSE_POST_MEDIA + "/" + mediaId;
    }
}
