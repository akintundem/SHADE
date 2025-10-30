package ai.eventplanner.event.controller;

import ai.eventplanner.event.dto.request.EventCollaboratorRequest;
import ai.eventplanner.event.dto.request.EventShareRequest;
import ai.eventplanner.event.dto.response.EventCollaboratorResponse;
import ai.eventplanner.event.dto.response.EventShareResponse;
import ai.eventplanner.event.dto.response.EventSharingOptionsResponse;
import ai.eventplanner.event.service.EventService;
import ai.eventplanner.user.entity.EventUser;
import ai.eventplanner.user.repo.EventUserRepository;
import ai.eventplanner.common.domain.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Event Collaboration and Sharing Controller
 * Handles event sharing, collaboration, and access control
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Collaboration", description = "Event sharing and collaboration operations")
@SecurityRequirement(name = "bearerAuth")
public class EventCollaborationController {

    private final EventService eventService;
    private final EventUserRepository eventUserRepository;

    public EventCollaborationController(EventService eventService, EventUserRepository eventUserRepository) {
        this.eventService = eventService;
        this.eventUserRepository = eventUserRepository;
    }

    // ==================== EVENT SHARING ENDPOINTS ====================

    @GetMapping("/{id}/share")
    @Operation(summary = "Get event sharing options", description = "Get available sharing options for an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sharing options retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<EventSharingOptionsResponse> getSharingOptions(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            EventSharingOptionsResponse response = new EventSharingOptionsResponse();
            response.setEventId(id);
            response.setAvailableChannels(Arrays.asList("email", "sms", "social", "link"));
            response.setShareLink(generateShareLink(id));
            response.setQrCodeAvailable(eventService.getById(id)
                    .map(event -> event.getQrCodeEnabled() && event.getQrCode() != null)
                    .orElse(false));
            response.setIsPublic(eventService.getById(id)
                    .map(event -> event.getIsPublic())
                    .orElse(false));
            response.setSocialMediaOptions(Arrays.asList("facebook", "twitter", "linkedin", "instagram"));
            response.setEmailOptions(Arrays.asList("invite", "reminder", "update"));
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Share event", description = "Share an event through various channels")
    public ResponseEntity<EventShareResponse> shareEvent(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventShareRequest request) {
        try {
            EventShareResponse response = new EventShareResponse();
            response.setShareId(UUID.randomUUID());
            response.setEventId(id);
            response.setChannel(request.getChannel());
            response.setRecipientCount(request.getRecipients() != null ? request.getRecipients().size() : 0);
            response.setStatus("sent");
            response.setShareLink(generateShareLink(id));
            response.setMessage(request.getMessage());
            response.setIncludeEventDetails(request.getIncludeEventDetails());
            response.setIncludeQRCode(request.getIncludeQRCode());
            response.setCreatedAt(LocalDateTime.now());
            
            // Simulate successful and failed recipients
            if (request.getRecipients() != null) {
                response.setSuccessfulRecipients(request.getRecipients());
                response.setFailedRecipients(new ArrayList<>());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== EVENT COLLABORATION ENDPOINTS ====================

    @GetMapping("/{id}/collaborators")
    @Operation(summary = "Get event collaborators", description = "Get list of event collaborators")
    public ResponseEntity<List<EventCollaboratorResponse>> getCollaborators(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        var eventUsersPage = eventUserRepository.findByEventId(id, pageable);
        List<EventUser> eventUsers = eventUsersPage.getContent();
        List<EventCollaboratorResponse> collaborators = new ArrayList<>();
        for (EventUser eu : eventUsers) {
            EventCollaboratorResponse r = new EventCollaboratorResponse();
            r.setCollaboratorId(eu.getId());
            r.setEventId(eu.getEventId());
            r.setUserId(eu.getUserId());
            r.setEmail(eu.getEmail());
            r.setRole(eu.getUserType());
            r.setPermissions(null);
            r.setNotes(eu.getNotes());
            collaborators.add(r);
        }
        return ResponseEntity.ok(collaborators);
    }

    @PostMapping("/{id}/collaborators")
    @Operation(summary = "Add event collaborator", description = "Add a new collaborator to an event")
    public ResponseEntity<EventCollaboratorResponse> addCollaborator(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventCollaboratorRequest request) {
        EventUser eu = new EventUser();
        eu.setEventId(id);
        eu.setUserId(request.getUserId());
        eu.setEmail(request.getEmail());
        eu.setUserType(request.getRole());
        eu.setRegistrationStatus(RegistrationStatus.CONFIRMED);
        eu.setRegistrationDate(LocalDateTime.now());
        eu.setNotes(request.getNotes());
        EventUser saved = eventUserRepository.save(eu);

        EventCollaboratorResponse response = new EventCollaboratorResponse();
        response.setCollaboratorId(saved.getId());
        response.setEventId(saved.getEventId());
        response.setUserId(saved.getUserId());
        response.setEmail(saved.getEmail());
        response.setRole(saved.getUserType());
        response.setPermissions(request.getPermissions());
        response.setNotes(saved.getNotes());
        response.setInvitationSent(request.getSendInvitation());
        response.setInvitationSentAt(request.getSendInvitation() ? LocalDateTime.now() : null);
        response.setAddedAt(saved.getCreatedAt());
        response.setUpdatedAt(saved.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/collaborators/{collaboratorId}")
    @Operation(summary = "Update event collaborator", description = "Update collaborator information")
    public ResponseEntity<EventCollaboratorResponse> updateCollaborator(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Collaborator ID") @PathVariable UUID collaboratorId,
            @Valid @RequestBody EventCollaboratorRequest request) {
        EventUser eu = eventUserRepository.findById(collaboratorId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found"));
        if (!eu.getEventId().equals(id)) {
            throw new IllegalArgumentException("Collaborator does not belong to event");
        }
        if (request.getUserId() != null) eu.setUserId(request.getUserId());
        if (request.getEmail() != null) eu.setEmail(request.getEmail());
        if (request.getRole() != null) eu.setUserType(request.getRole());
        if (request.getNotes() != null) eu.setNotes(request.getNotes());
        EventUser saved = eventUserRepository.save(eu);

        EventCollaboratorResponse response = new EventCollaboratorResponse();
        response.setCollaboratorId(saved.getId());
        response.setEventId(saved.getEventId());
        response.setUserId(saved.getUserId());
        response.setEmail(saved.getEmail());
        response.setRole(saved.getUserType());
        response.setPermissions(request.getPermissions());
        response.setNotes(saved.getNotes());
        response.setUpdatedAt(saved.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/collaborators/{collaboratorId}")
    @Operation(summary = "Remove event collaborator", description = "Remove a collaborator from an event")
    public ResponseEntity<Void> removeCollaborator(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Collaborator ID") @PathVariable UUID collaboratorId) {
        EventUser eu = eventUserRepository.findById(collaboratorId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found"));
        if (!eu.getEventId().equals(id)) {
            throw new IllegalArgumentException("Collaborator does not belong to event");
        }
        eventUserRepository.delete(eu);
        return ResponseEntity.noContent().build();
    }

    // ==================== HELPER METHODS ====================

    private String generateShareLink(UUID eventId) {
        return "https://eventplanner.app/events/" + eventId.toString();
    }

    
}
