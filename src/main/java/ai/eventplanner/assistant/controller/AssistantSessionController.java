package ai.eventplanner.assistant.controller;

import ai.eventplanner.assistant.entity.AssistantSessionEntity;
import ai.eventplanner.assistant.service.AssistantSessionService;
import ai.eventplanner.common.domain.enums.SessionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for assistant session management
 */
@RestController
@RequestMapping("/api/assistant/sessions")
public class AssistantSessionController {
    
    private final AssistantSessionService assistantSessionService;
    
    public AssistantSessionController(AssistantSessionService assistantSessionService) {
        this.assistantSessionService = assistantSessionService;
    }
    
    /**
     * Get or create session for event
     */
    @PostMapping("/event/{eventId}")
    public ResponseEntity<AssistantSessionEntity> getOrCreateSessionForEvent(
            @PathVariable UUID eventId,
            @RequestParam String eventName,
            Authentication authentication) {
        
        // Get organizer ID from authentication
        UUID organizerId = getOrganizerIdFromAuthentication(authentication);
        
        AssistantSessionEntity session = assistantSessionService.getOrCreateSessionForEvent(
                eventId, organizerId, eventName);
        
        return ResponseEntity.ok(session);
    }
    
    /**
     * Get session for event
     */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<AssistantSessionEntity> getSessionForEvent(@PathVariable UUID eventId) {
        Optional<AssistantSessionEntity> session = assistantSessionService.getSessionForEvent(eventId);
        
        if (session.isPresent()) {
            return ResponseEntity.ok(session.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Check if session exists for event
     */
    @GetMapping("/event/{eventId}/exists")
    public ResponseEntity<Boolean> hasSessionForEvent(@PathVariable UUID eventId) {
        boolean exists = assistantSessionService.hasSessionForEvent(eventId);
        return ResponseEntity.ok(exists);
    }
    
    /**
     * Create new session for event (fails if already exists)
     */
    @PostMapping("/event/{eventId}/create")
    public ResponseEntity<AssistantSessionEntity> createSessionForEvent(
            @PathVariable UUID eventId,
            @RequestParam String eventName,
            Authentication authentication) {
        
        try {
            UUID organizerId = getOrganizerIdFromAuthentication(authentication);
            AssistantSessionEntity session = assistantSessionService.createSessionForEvent(
                    eventId, organizerId, eventName);
            return ResponseEntity.ok(session);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Update session
     */
    @PutMapping("/{sessionId}")
    public ResponseEntity<AssistantSessionEntity> updateSession(
            @PathVariable UUID sessionId,
            @RequestBody AssistantSessionEntity session) {
        
        session.setId(sessionId);
        AssistantSessionEntity updatedSession = assistantSessionService.updateSession(session);
        return ResponseEntity.ok(updatedSession);
    }
    
    /**
     * Get sessions by organizer
     */
    @GetMapping("/organizer")
    public ResponseEntity<List<AssistantSessionEntity>> getSessionsByOrganizer(Authentication authentication) {
        UUID organizerId = getOrganizerIdFromAuthentication(authentication);
        List<AssistantSessionEntity> sessions = assistantSessionService.getSessionsByOrganizer(organizerId);
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Delete session for event
     */
    @DeleteMapping("/event/{eventId}")
    public ResponseEntity<Void> deleteSessionForEvent(@PathVariable UUID eventId) {
        assistantSessionService.deleteSessionForEvent(eventId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Update session status
     */
    @PutMapping("/event/{eventId}/status")
    public ResponseEntity<AssistantSessionEntity> updateSessionStatus(
            @PathVariable UUID eventId,
            @RequestParam SessionStatus status) {
        
        try {
            AssistantSessionEntity session = assistantSessionService.updateSessionStatus(eventId, status);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Extract organizer ID from authentication
     */
    private UUID getOrganizerIdFromAuthentication(Authentication authentication) {
        // This would need to be implemented based on your authentication setup
        // For now, returning a placeholder
        return UUID.randomUUID();
    }
}
