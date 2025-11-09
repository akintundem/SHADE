package eventplanner.assistant.controller;

import eventplanner.assistant.entity.AssistantSessionEntity;
import eventplanner.assistant.service.AssistantSessionService;
import eventplanner.common.domain.enums.SessionStatus;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#eventId"})
    public ResponseEntity<AssistantSessionEntity> getOrCreateSessionForEvent(
            @PathVariable UUID eventId,
            @RequestParam String eventName,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        UUID organizerId = requireUserId(principal);
        
        AssistantSessionEntity session = assistantSessionService.getOrCreateSessionForEvent(
                eventId, organizerId, eventName);
        
        return ResponseEntity.ok(session);
    }
    
    /**
     * Get session for event
     */
    @GetMapping("/event/{eventId}")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#eventId"})
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
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#eventId"})
    public ResponseEntity<Boolean> hasSessionForEvent(@PathVariable UUID eventId) {
        boolean exists = assistantSessionService.hasSessionForEvent(eventId);
        return ResponseEntity.ok(exists);
    }
    
    /**
     * Create new session for event (fails if already exists)
     */
    @PostMapping("/event/{eventId}/create")
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#eventId"})
    public ResponseEntity<AssistantSessionEntity> createSessionForEvent(
            @PathVariable UUID eventId,
            @RequestParam String eventName,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        try {
            UUID organizerId = requireUserId(principal);
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
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#session.eventId"})
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
    @RequiresPermission(value = RbacPermissions.MY_EVENTS_READ, resources = {"user_id=#principal.id"})
    public ResponseEntity<List<AssistantSessionEntity>> getSessionsByOrganizer(@AuthenticationPrincipal UserPrincipal principal) {
        UUID organizerId = requireUserId(principal);
        List<AssistantSessionEntity> sessions = assistantSessionService.getSessionsByOrganizer(organizerId);
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Delete session for event
     */
    @DeleteMapping("/event/{eventId}")
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#eventId"})
    public ResponseEntity<Void> deleteSessionForEvent(@PathVariable UUID eventId) {
        assistantSessionService.deleteSessionForEvent(eventId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Update session status
     */
    @PutMapping("/event/{eventId}/status")
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#eventId"})
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
    
    private UUID requireUserId(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal.getId();
    }
}
