package ai.eventplanner.event.repo;

import ai.eventplanner.event.model.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {
    List<ConversationEntity> findByEventIdOrderByCreatedAtAsc(UUID eventId);
    List<ConversationEntity> findByEventIdOrderByCreatedAtDesc(UUID eventId);
    List<ConversationEntity> findByEventIdAndUserIdOrderByCreatedAtDesc(UUID eventId, UUID userId);
}


