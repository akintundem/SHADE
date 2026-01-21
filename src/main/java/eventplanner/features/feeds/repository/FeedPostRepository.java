package eventplanner.features.feeds.repository;

import eventplanner.common.storage.s3.dto.MediaUploadStatus;
import eventplanner.features.feeds.entity.EventFeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeedPostRepository extends JpaRepository<EventFeedPost, UUID> {
    @Query("SELECT p FROM EventFeedPost p WHERE p.event.id = :eventId ORDER BY p.createdAt DESC")
    List<EventFeedPost> findByEventIdOrderByCreatedAtDesc(@Param("eventId") UUID eventId);
    
    /**
     * Find only completed posts (excludes PENDING/FAILED uploads) with pagination
     */
    @Query("SELECT p FROM EventFeedPost p WHERE p.event.id = :eventId AND p.mediaUploadStatus = :status ORDER BY p.createdAt DESC")
    Page<EventFeedPost> findByEventIdAndMediaUploadStatusOrderByCreatedAtDesc(
            @Param("eventId") UUID eventId, 
            @Param("status") MediaUploadStatus status,
            Pageable pageable
    );
    
    /**
     * Find all posts for an event with pagination
     */
    @Query("SELECT p FROM EventFeedPost p WHERE p.event.id = :eventId ORDER BY p.createdAt DESC")
    Page<EventFeedPost> findByEventIdOrderByCreatedAtDesc(@Param("eventId") UUID eventId, Pageable pageable);
    
    /**
     * Find incomplete posts older than specified time for cleanup
     */
    List<EventFeedPost> findByMediaUploadStatusAndCreatedAtBefore(
            MediaUploadStatus status,
            LocalDateTime before
    );
    
    /**
     * Find all posts created by a user
     */
    @Query("SELECT p FROM EventFeedPost p WHERE p.createdBy.id = :userId AND p.mediaUploadStatus = :status ORDER BY p.createdAt DESC")
    Page<EventFeedPost> findByCreatedByUserIdAndMediaUploadStatusOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            @Param("status") MediaUploadStatus status,
            Pageable pageable
    );

    /**
     * Check if user has already reposted a specific post
     */
    @Query("SELECT COUNT(p) > 0 FROM EventFeedPost p WHERE p.createdBy.id = :userId AND p.repostedFrom.id = :originalPostId")
    boolean existsByCreatedByIdAndRepostedFromId(
            @Param("userId") UUID userId,
            @Param("originalPostId") UUID originalPostId
    );
}


