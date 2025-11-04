package eventplanner.assistant.repository;

import eventplanner.assistant.entity.AssistantMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssistantMessageRepository extends JpaRepository<AssistantMessageEntity, UUID> {

    List<AssistantMessageEntity> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    void deleteBySessionId(UUID sessionId);
}
