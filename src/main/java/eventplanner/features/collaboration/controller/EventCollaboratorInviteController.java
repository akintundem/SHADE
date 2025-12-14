package eventplanner.features.collaboration.controller;

import eventplanner.features.collaboration.service.EventCollaboratorInviteService;
import eventplanner.features.event.dto.request.CreateCollaboratorInviteRequest;
import eventplanner.features.event.dto.request.RespondToCollaboratorInviteRequest;
import eventplanner.features.event.dto.response.CollaboratorInviteResponse;
import eventplanner.features.collaboration.dto.response.EventCollaboratorResponse;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Event Collaborator Invites", description = "Invite/accept/decline collaborator access to events")
@SecurityRequirement(name = "bearerAuth")
public class EventCollaboratorInviteController {

    private final EventCollaboratorInviteService inviteService;
    private final AuthorizationService authorizationService;

    public EventCollaboratorInviteController(
            EventCollaboratorInviteService inviteService,
            AuthorizationService authorizationService
    ) {
        this.inviteService = inviteService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/api/v1/events/{eventId}/collaborator-invites")
    @RequiresPermission(value = RbacPermissions.ROLE_ASSIGN, resources = {"event_id=#eventId"})
    @Operation(summary = "Create collaborator invite", description = "Invite a user (by userId/email) to collaborate on an event")
    public ResponseEntity<CollaboratorInviteResponse> createInvite(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCollaboratorInviteRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (!authorizationService.isEventOwner(principal, eventId) && !authorizationService.isAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only event owners or admins can invite collaborators");
        }
        try {
            CollaboratorInviteResponse response = inviteService.createInvite(eventId, principal, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/api/v1/events/{eventId}/collaborator-invites")
    @RequiresPermission(value = RbacPermissions.ROLE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "List event collaborator invites", description = "List pending/previous collaborator invites for an event (owner/admin)")
    public ResponseEntity<Page<CollaboratorInviteResponse>> listEventInvites(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (!authorizationService.isEventOwner(principal, eventId) && !authorizationService.isAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only event owners or admins can view invites");
        }
        return ResponseEntity.ok(inviteService.listEventInvites(eventId, page, size));
    }

    @DeleteMapping("/api/v1/events/{eventId}/collaborator-invites/{inviteId}")
    @RequiresPermission(value = RbacPermissions.ROLE_ASSIGN, resources = {"event_id=#eventId"})
    @Operation(summary = "Revoke collaborator invite", description = "Revoke a pending collaborator invite")
    public ResponseEntity<Void> revokeInvite(
            @PathVariable UUID eventId,
            @PathVariable UUID inviteId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (!authorizationService.isEventOwner(principal, eventId) && !authorizationService.isAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only event owners or admins can revoke invites");
        }
        try {
            inviteService.revokeInvite(inviteId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/api/v1/collaborator-invites/incoming")
    @Operation(summary = "List my collaborator invites", description = "List pending collaborator invites for the authenticated user")
    public ResponseEntity<List<CollaboratorInviteResponse>> myInvites(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            return ResponseEntity.ok(inviteService.listMyPendingInvites(principal));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/api/v1/collaborator-invites/{inviteId}/accept")
    @Operation(summary = "Accept collaborator invite", description = "Accept a collaborator invite (in-app)")
    public ResponseEntity<EventCollaboratorResponse> acceptInvite(
            @PathVariable UUID inviteId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) RespondToCollaboratorInviteRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            var membership = inviteService.acceptInviteById(inviteId, principal);
            EventCollaboratorResponse response = new EventCollaboratorResponse();
            response.setCollaboratorId(membership.getId());
            response.setEventId(membership.getEvent() != null ? membership.getEvent().getId() : null);
            response.setUserId(membership.getUser() != null ? membership.getUser().getId() : null);
            response.setEmail(membership.getEmail());
            response.setUserName(membership.getName());
            response.setRole(membership.getUserType());
            response.setRegistrationStatus(membership.getRegistrationStatus() != null ? membership.getRegistrationStatus().name() : null);
            response.setAddedAt(membership.getCreatedAt());
            response.setUpdatedAt(membership.getUpdatedAt());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/api/v1/collaborator-invites/accept")
    @Operation(summary = "Accept collaborator invite by token", description = "Accept a collaborator invite using an email token (requires authentication)")
    public ResponseEntity<EventCollaboratorResponse> acceptInviteByToken(
            @RequestParam String token,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            var membership = inviteService.acceptInviteByToken(token, principal);
            EventCollaboratorResponse r = new EventCollaboratorResponse();
            r.setCollaboratorId(membership.getId());
            r.setEventId(membership.getEvent() != null ? membership.getEvent().getId() : null);
            r.setUserId(membership.getUser() != null ? membership.getUser().getId() : null);
            r.setEmail(membership.getEmail());
            r.setUserName(membership.getName());
            r.setRole(membership.getUserType());
            r.setRegistrationStatus(membership.getRegistrationStatus() != null ? membership.getRegistrationStatus().name() : null);
            r.setAddedAt(membership.getCreatedAt());
            r.setUpdatedAt(membership.getUpdatedAt());
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/api/v1/collaborator-invites/{inviteId}/decline")
    @Operation(summary = "Decline collaborator invite", description = "Decline a collaborator invite")
    public ResponseEntity<Void> declineInvite(
            @PathVariable UUID inviteId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) RespondToCollaboratorInviteRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            inviteService.declineInvite(inviteId, principal);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}





