package ai.eventplanner.assistant.repository;

import ai.eventplanner.assistant.entity.AssistantSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for assistant sessions
 */
@Repository
public interface AssistantSessionRepository extends JpaRepository<AssistantSessionEntity, UUID> {
    
    /**
     * Find session by event ID (should be unique)
     */
    Optional<AssistantSessionEntity> findByEventId(UUID eventId);
    
    /**
     * Check if session exists for event
     */
    boolean existsByEventId(UUID eventId);
    
    /**
     * Find sessions by organizer ID
     */
    java.util.List<AssistantSessionEntity> findByOrganizerId(UUID organizerId);
}