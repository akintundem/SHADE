package eventplanner.features.feeds.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
}


