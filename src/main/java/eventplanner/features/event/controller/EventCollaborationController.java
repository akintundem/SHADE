package eventplanner.features.event.controller;

import eventplanner.features.event.dto.request.EventCollaboratorRequest;
import eventplanner.features.event.dto.response.EventCollaboratorResponse;
import eventplanner.features.event.service.EventCollaboratorService;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Event Collaboration Controller handling collaborator management.
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Collaboration", description = "Event collaboration operations")
@SecurityRequirement(name = "bearerAuth")
public class EventCollaborationController {

    private final EventCollaboratorService collaboratorService;

    public EventCollaborationController(EventCollaboratorService collaboratorService) {
        this.collaboratorService = collaboratorService;
    }

    @GetMapping("/{id}/collaborators")
    @RequiresPermission(value = RbacPermissions.ROLE_READ, resources = {"event_id=#id"})
    @Operation(summary = "Get event collaborators", description = "Get list of event collaborators")
    public ResponseEntity<List<EventCollaboratorResponse>> getCollaborators(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        List<EventCollaboratorResponse> collaborators = collaboratorService.getCollaborators(id, page, size);
        return ResponseEntity.ok(collaborators);
    }

    @PostMapping("/{id}/collaborators")
    @RequiresPermission(value = RbacPermissions.ROLE_ASSIGN, resources = {"event_id=#id"})
    @Operation(summary = "Add event collaborator", description = "Add a new collaborator to an event")
    public ResponseEntity<EventCollaboratorResponse> addCollaborator(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventCollaboratorRequest request) {
        EventCollaboratorResponse response = collaboratorService.addCollaborator(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/collaborators/{collaboratorId}")
    @RequiresPermission(value = RbacPermissions.ROLE_UPDATE, resources = {"event_id=#id"})
    @Operation(summary = "Update event collaborator", description = "Update collaborator information")
    public ResponseEntity<EventCollaboratorResponse> updateCollaborator(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Collaborator ID") @PathVariable UUID collaboratorId,
            @Valid @RequestBody EventCollaboratorRequest request) {
        EventCollaboratorResponse response = collaboratorService.updateCollaborator(id, collaboratorId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/collaborators/{collaboratorId}")
    @RequiresPermission(value = RbacPermissions.ROLE_REMOVE, resources = {"event_id=#id"})
    @Operation(summary = "Remove event collaborator", description = "Remove a collaborator from an event")
    public ResponseEntity<Void> removeCollaborator(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Collaborator ID") @PathVariable UUID collaboratorId) {
        collaboratorService.removeCollaborator(id, collaboratorId);
        return ResponseEntity.noContent().build();
    }
}
