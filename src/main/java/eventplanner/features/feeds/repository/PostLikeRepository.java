package eventplanner.features.feeds.repository;

import eventplanner.features.feeds.entity.EventFeedPost;
import eventplanner.features.feeds.entity.PostLike;
import eventplanner.security.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    boolean existsByPostAndUser(EventFeedPost post, UserAccount user);

    long countByPost(EventFeedPost post);

    void deleteByPostAndUser(EventFeedPost post, UserAccount user);

    /**
     * Batch count likes for multiple posts in a single query.
     */
    @Query("SELECT l.post.id, COUNT(l) FROM PostLike l WHERE l.post.id IN :postIds GROUP BY l.post.id")
    List<Object[]> countByPostIds(@Param("postIds") List<UUID> postIds);

    /**
     * Batch check which posts are liked by a specific user.
     */
    @Query("SELECT l.post.id FROM PostLike l WHERE l.post.id IN :postIds AND l.user.id = :userId")
    List<UUID> findLikedPostIdsByUserIdAndPostIds(@Param("userId") UUID userId, @Param("postIds") List<UUID> postIds);
}
