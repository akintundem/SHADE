package eventplanner.features.feeds.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_likes", indexes = {
    @Index(name = "idx_post_likes_post_id", columnList = "post_id"),
    @Index(name = "idx_post_likes_user_id", columnList = "user_id"),
    @Index(name = "idx_post_likes_post_user", columnList = "post_id,user_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private EventFeedPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
}
