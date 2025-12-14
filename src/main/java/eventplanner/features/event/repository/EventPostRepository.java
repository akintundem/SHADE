package eventplanner.features.event.repository;

import eventplanner.features.event.entity.EventPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventPostRepository extends JpaRepository<EventPost, UUID> {
    List<EventPost> findByEventIdOrderByCreatedAtDesc(UUID eventId);
}


