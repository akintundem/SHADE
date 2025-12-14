package eventplanner.features.feeds.repository;

import eventplanner.features.feeds.entity.EventFeedPost;
import eventplanner.features.feeds.entity.PostLike;
import eventplanner.security.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {
    // UUID-based queries (for backward compatibility and performance)
    Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId);
    boolean existsByPostIdAndUserId(UUID postId, UUID userId);
    long countByPostId(UUID postId);
    void deleteByPostIdAndUserId(UUID postId, UUID userId);
    
    // Relationship-based queries
    Optional<PostLike> findByPostAndUser(EventFeedPost post, UserAccount user);
    boolean existsByPostAndUser(EventFeedPost post, UserAccount user);
    long countByPost(EventFeedPost post);
    void deleteByPostAndUser(EventFeedPost post, UserAccount user);
}
