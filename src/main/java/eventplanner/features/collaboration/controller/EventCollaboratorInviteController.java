package eventplanner.features.collaboration.controller;

import eventplanner.features.collaboration.service.EventCollaboratorInviteService;
import eventplanner.features.event.dto.request.AcceptCollaboratorInviteByTokenRequest;
import eventplanner.features.event.dto.request.CreateCollaboratorInviteRequest;
import eventplanner.features.event.dto.request.RespondToCollaboratorInviteRequest;
import eventplanner.features.event.dto.response.CollaboratorInviteResponse;
import eventplanner.features.collaboration.dto.response.EventCollaboratorResponse;
import eventplanner.common.util.Preconditions;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
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
        Preconditions.requireAuthenticated(principal);
        if (!authorizationService.canManageEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only event owners or admins can invite collaborators");
        }
        CollaboratorInviteResponse response = inviteService.createInvite(eventId, principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        Preconditions.requireAuthenticated(principal);
        if (!authorizationService.canManageEvent(principal, eventId)) {
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
        Preconditions.requireAuthenticated(principal);
        if (!authorizationService.canManageEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only event owners or admins can revoke invites");
        }
        inviteService.revokeInvite(inviteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/collaborator-invites/incoming")
    // No @RequiresPermission - any authenticated user can view their own pending invites
    @Operation(summary = "List my collaborator invites", description = "List pending collaborator invites for the authenticated user (paginated, excludes expired)")
    public ResponseEntity<org.springframework.data.domain.Page<CollaboratorInviteResponse>> myInvites(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Preconditions.requireAuthenticated(principal);
        return ResponseEntity.ok(inviteService.listMyPendingInvites(principal, page, size));
    }

    @PostMapping("/api/v1/collaborator-invites/{inviteId}/accept")
    @RequiresPermission(value = RbacPermissions.COLLABORATOR_INVITE_ACCEPT, resources = {"user_id=#principal.id"})
    @Operation(summary = "Accept collaborator invite", description = "Accept a collaborator invite (in-app)")
    public ResponseEntity<EventCollaboratorResponse> acceptInvite(
            @PathVariable UUID inviteId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) RespondToCollaboratorInviteRequest request
    ) {
        Preconditions.requireAuthenticated(principal);
        var membership = inviteService.acceptInviteById(inviteId, principal);
        EventCollaboratorResponse response = new EventCollaboratorResponse();
        response.setCollaboratorId(membership.getId());
        response.setEventId(membership.getEvent() != null ? membership.getEvent().getId() : null);
        response.setUserId(membership.getUser() != null ? membership.getUser().getId() : null);
        response.setEmail(membership.getUser() != null ? membership.getUser().getEmail() : null);
        response.setUserName(membership.getUser() != null ? membership.getUser().getName() : null);
        response.setRole(membership.getUserType());
        response.setRegistrationStatus(membership.getRegistrationStatus() != null ? membership.getRegistrationStatus().name() : null);
        response.setAddedAt(membership.getCreatedAt());
        response.setUpdatedAt(membership.getUpdatedAt());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/collaborator-invites/accept")
    @RequiresPermission(value = RbacPermissions.COLLABORATOR_INVITE_ACCEPT, resources = {"user_id=#principal.id"})
    @Operation(summary = "Accept collaborator invite by token", description = "Accept a collaborator invite using an email token (requires authentication). Token is passed in the request body to avoid logging in server/proxy URL logs.")
    public ResponseEntity<EventCollaboratorResponse> acceptInviteByToken(
            @Valid @RequestBody AcceptCollaboratorInviteByTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Preconditions.requireAuthenticated(principal);
        var membership = inviteService.acceptInviteByToken(request.getToken(), principal);
        EventCollaboratorResponse r = new EventCollaboratorResponse();
        r.setCollaboratorId(membership.getId());
        r.setEventId(membership.getEvent() != null ? membership.getEvent().getId() : null);
        r.setUserId(membership.getUser() != null ? membership.getUser().getId() : null);
        r.setEmail(membership.getUser() != null ? membership.getUser().getEmail() : null);
        r.setUserName(membership.getUser() != null ? membership.getUser().getName() : null);
        r.setRole(membership.getUserType());
        r.setRegistrationStatus(membership.getRegistrationStatus() != null ? membership.getRegistrationStatus().name() : null);
        r.setAddedAt(membership.getCreatedAt());
        r.setUpdatedAt(membership.getUpdatedAt());
        return ResponseEntity.ok(r);
    }

    @PostMapping("/api/v1/collaborator-invites/{inviteId}/decline")
    @RequiresPermission(value = RbacPermissions.ROLE_READ, resources = {"invite_id=#inviteId"})
    @Operation(summary = "Decline collaborator invite", description = "Decline a collaborator invite")
    public ResponseEntity<Void> declineInvite(
            @PathVariable UUID inviteId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) RespondToCollaboratorInviteRequest request
    ) {
        Preconditions.requireAuthenticated(principal);
        inviteService.declineInvite(inviteId, principal);
        return ResponseEntity.noContent().build();
    }
}
