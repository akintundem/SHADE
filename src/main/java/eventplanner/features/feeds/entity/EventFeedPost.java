package eventplanner.features.feeds.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.storage.upload.MediaUploadStatus;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "event_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventFeedPost extends BaseEntity {

    public enum PostType {
        TEXT,
        IMAGE,
        VIDEO
    }

    /**
     * Many-to-one relationship with the event this post belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false)
    private PostType postType = PostType.TEXT;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Optional reference to a stored object (S3) representing the media for this post.
     * For text-only posts this is null.
     */
    @Column(name = "media_object_id")
    private UUID mediaObjectId;

    /**
     * Many-to-one relationship with the user who created this post.
     * Optional as posts might be created by system or during migration.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    /**
     * Status of media upload for IMAGE/VIDEO posts.
     * TEXT posts are always COMPLETED.
     * IMAGE/VIDEO posts start as PENDING until media upload completes.
     * 
     * Note: Initially nullable to allow Hibernate to add the column to existing tables.
     * Existing rows will be updated to COMPLETED on first access.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "media_upload_status")
    private MediaUploadStatus mediaUploadStatus = MediaUploadStatus.COMPLETED;

    /**
     * One-to-many relationship with post likes.
     * Lazy loaded to avoid N+1 queries.
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PostLike> likes = new ArrayList<>();

    /**
     * One-to-many relationship with post comments.
     * Lazy loaded to avoid N+1 queries.
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PostComment> comments = new ArrayList<>();

    /**
     * Ensure mediaUploadStatus is never null for new/updated rows
     */
    @PrePersist
    @PreUpdate
    private void ensureMediaUploadStatus() {
        if (mediaUploadStatus == null) {
            // For existing posts, default to COMPLETED (they were created before status tracking)
            // For new posts, this should already be set by the service
            mediaUploadStatus = MediaUploadStatus.COMPLETED;
        }
    }
}


