package ai.eventplanner.event.controller;

import ai.eventplanner.event.dto.request.EventCollaboratorRequest;
import ai.eventplanner.event.dto.request.EventShareRequest;
import ai.eventplanner.event.dto.response.EventCollaboratorResponse;
import ai.eventplanner.event.dto.response.EventShareResponse;
import ai.eventplanner.event.service.EventService;
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

    public EventCollaborationController(EventService eventService) {
        this.eventService = eventService;
    }

    // ==================== EVENT SHARING ENDPOINTS ====================

    @GetMapping("/{id}/share")
    @Operation(summary = "Get event sharing options", description = "Get available sharing options for an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sharing options retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getSharingOptions(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Map<String, Object> options = Map.of(
                    "eventId", id,
                    "availableChannels", Arrays.asList("email", "sms", "social", "link"),
                    "shareLink", generateShareLink(id),
                    "qrCodeAvailable", eventService.getById(id)
                            .map(event -> event.getQrCodeEnabled() && event.getQrCode() != null)
                            .orElse(false),
                    "isPublic", eventService.getById(id)
                            .map(event -> event.getIsPublic())
                            .orElse(false)
            );
            return ResponseEntity.ok(options);
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
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            // For now, return empty list - this would be populated from EventUser table
            List<EventCollaboratorResponse> collaborators = new ArrayList<>();
            return ResponseEntity.ok(collaborators);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/collaborators")
    @Operation(summary = "Add event collaborator", description = "Add a new collaborator to an event")
    public ResponseEntity<EventCollaboratorResponse> addCollaborator(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventCollaboratorRequest request) {
        try {
            EventCollaboratorResponse response = new EventCollaboratorResponse();
            response.setCollaboratorId(UUID.randomUUID());
            response.setEventId(id);
            response.setUserId(request.getUserId());
            response.setEmail(request.getEmail());
            response.setRole(request.getRole());
            response.setPermissions(request.getPermissions());
            response.setNotes(request.getNotes());
            response.setInvitationSent(request.getSendInvitation());
            response.setInvitationSentAt(request.getSendInvitation() ? LocalDateTime.now() : null);
            response.setAddedAt(LocalDateTime.now());
            response.setUpdatedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/{id}/collaborators/{collaboratorId}")
    @Operation(summary = "Update event collaborator", description = "Update collaborator information")
    public ResponseEntity<EventCollaboratorResponse> updateCollaborator(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Collaborator ID") @PathVariable UUID collaboratorId,
            @Valid @RequestBody EventCollaboratorRequest request) {
        try {
            EventCollaboratorResponse response = new EventCollaboratorResponse();
            response.setCollaboratorId(collaboratorId);
            response.setEventId(id);
            response.setUserId(request.getUserId());
            response.setEmail(request.getEmail());
            response.setRole(request.getRole());
            response.setPermissions(request.getPermissions());
            response.setNotes(request.getNotes());
            response.setUpdatedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/{id}/collaborators/{collaboratorId}")
    @Operation(summary = "Remove event collaborator", description = "Remove a collaborator from an event")
    public ResponseEntity<Void> removeCollaborator(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Collaborator ID") @PathVariable UUID collaboratorId) {
        try {
            // In a real implementation, this would remove the collaborator from the database
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== HELPER METHODS ====================

    private String generateShareLink(UUID eventId) {
        return "https://eventplanner.app/events/" + eventId.toString();
    }
}
