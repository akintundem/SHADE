package eventplanner.features.feeds.repository;

import eventplanner.common.storage.upload.MediaUploadStatus;
import eventplanner.features.feeds.entity.EventFeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeedPostRepository extends JpaRepository<EventFeedPost, UUID> {
    List<EventFeedPost> findByEventIdOrderByCreatedAtDesc(UUID eventId);
    
    /**
     * Find only completed posts (excludes PENDING/FAILED uploads) with pagination
     */
    Page<EventFeedPost> findByEventIdAndMediaUploadStatusOrderByCreatedAtDesc(
            UUID eventId, 
            MediaUploadStatus status,
            Pageable pageable
    );
    
    /**
     * Find all posts for an event with pagination
     */
    Page<EventFeedPost> findByEventIdOrderByCreatedAtDesc(UUID eventId, Pageable pageable);
    
    /**
     * Find incomplete posts older than specified time for cleanup
     */
    List<EventFeedPost> findByMediaUploadStatusAndCreatedAtBefore(
            MediaUploadStatus status,
            LocalDateTime before
    );
}


