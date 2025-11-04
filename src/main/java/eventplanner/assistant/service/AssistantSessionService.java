package eventplanner.assistant.service;

import eventplanner.assistant.entity.AssistantSessionEntity;
import eventplanner.assistant.repository.AssistantSessionRepository;
import eventplanner.common.domain.enums.SessionStatus;
import eventplanner.common.domain.enums.SessionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing assistant sessions with one-chat-per-event constraint
 */
@Service
@Transactional
public class AssistantSessionService {
    
    private final AssistantSessionRepository assistantSessionRepository;
    
    public AssistantSessionService(AssistantSessionRepository assistantSessionRepository) {
        this.assistantSessionRepository = assistantSessionRepository;
    }
    
    /**
     * Get or create session for event (ensures only one session per event)
     */
    public AssistantSessionEntity getOrCreateSessionForEvent(UUID eventId, UUID organizerId, String eventName) {
        // Try to find existing session
        Optional<AssistantSessionEntity> existingSession = assistantSessionRepository.findByEventId(eventId);
        
        if (existingSession.isPresent()) {
            return existingSession.get();
        }
        
        // Create new session if none exists
        AssistantSessionEntity session = new AssistantSessionEntity();
        session.setEventId(eventId);
        session.setOrganizerId(organizerId);
        session.setDomain("event_planning");
        session.setName("Chat for: " + eventName);
        session.setType(SessionType.EVENT_PLANNING);
        session.setDate(OffsetDateTime.now());
        session.setStatus(SessionStatus.ACTIVE);
        session.setAiGenerated(false);
        
        return assistantSessionRepository.save(session);
    }
    
    /**
     * Get session for event (returns null if not found)
     */
    public Optional<AssistantSessionEntity> getSessionForEvent(UUID eventId) {
        return assistantSessionRepository.findByEventId(eventId);
    }
    
    /**
     * Check if session exists for event
     */
    public boolean hasSessionForEvent(UUID eventId) {
        return assistantSessionRepository.existsByEventId(eventId);
    }
    
    /**
     * Create session for event (throws exception if already exists)
     */
    public AssistantSessionEntity createSessionForEvent(UUID eventId, UUID organizerId, String eventName) {
        if (assistantSessionRepository.existsByEventId(eventId)) {
            throw new IllegalStateException("Session already exists for event: " + eventId);
        }
        
        AssistantSessionEntity session = new AssistantSessionEntity();
        session.setEventId(eventId);
        session.setOrganizerId(organizerId);
        session.setDomain("event_planning");
        session.setName("Chat for: " + eventName);
        session.setType(SessionType.EVENT_PLANNING);
        session.setDate(OffsetDateTime.now());
        session.setStatus(SessionStatus.ACTIVE);
        session.setAiGenerated(false);
        
        return assistantSessionRepository.save(session);
    }
    
    /**
     * Update session
     */
    public AssistantSessionEntity updateSession(AssistantSessionEntity session) {
        return assistantSessionRepository.save(session);
    }
    
    /**
     * Get sessions by organizer
     */
    public List<AssistantSessionEntity> getSessionsByOrganizer(UUID organizerId) {
        return assistantSessionRepository.findByOrganizerId(organizerId);
    }
    
    /**
     * Delete session for event
     */
    public void deleteSessionForEvent(UUID eventId) {
        assistantSessionRepository.findByEventId(eventId)
                .ifPresent(assistantSessionRepository::delete);
    }
    
    /**
     * Update session status
     */
    public AssistantSessionEntity updateSessionStatus(UUID eventId, SessionStatus status) {
        AssistantSessionEntity session = assistantSessionRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Session not found for event: " + eventId));
        
        session.setStatus(status);
        return assistantSessionRepository.save(session);
    }
}
