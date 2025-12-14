package eventplanner.features.feeds.repository;

import eventplanner.features.feeds.entity.EventFeedPost;
import eventplanner.features.feeds.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {
    Page<PostComment> findByPostIdOrderByCreatedAtAsc(UUID postId, Pageable pageable);
    long countByPostId(UUID postId);
    
    // Relationship-based queries
    Page<PostComment> findByPostOrderByCreatedAtAsc(EventFeedPost post, Pageable pageable);
    long countByPost(EventFeedPost post);
}
