package eventplanner.features.event.repository;

import eventplanner.features.event.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByEventIdOrderByCreatedAtAsc(UUID eventId);
    List<Conversation> findByEventIdOrderByCreatedAtDesc(UUID eventId);
    List<Conversation> findByEventIdAndUserIdOrderByCreatedAtDesc(UUID eventId, UUID userId);
}


