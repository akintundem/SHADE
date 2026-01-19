package eventplanner.features.feeds.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_comments", indexes = {
    @Index(name = "idx_post_comments_post_id", columnList = "post_id"),
    @Index(name = "idx_post_comments_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private EventFeedPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}
