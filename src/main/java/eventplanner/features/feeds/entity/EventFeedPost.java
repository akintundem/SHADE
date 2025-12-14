package eventplanner.features.feeds.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.storage.upload.MediaUploadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

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

    @Column(name = "created_by")
    private UUID createdBy;

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


