package eventplanner.features.feeds.repository;

import eventplanner.features.feeds.entity.EventFeedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedPostRepository extends JpaRepository<EventFeedPost, UUID> {
    List<EventFeedPost> findByEventIdOrderByCreatedAtDesc(UUID eventId);
}


