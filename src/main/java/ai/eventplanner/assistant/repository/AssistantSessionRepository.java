package ai.eventplanner.assistant.repository;

import ai.eventplanner.assistant.entity.AssistantSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssistantSessionRepository extends JpaRepository<AssistantSessionEntity, UUID> {
}
