package eventplanner.features.attendee.controller;

import eventplanner.features.attendee.dto.request.CreateAttendeeInviteRequest;
import eventplanner.features.attendee.dto.response.AttendeeInviteResponse;
import eventplanner.features.attendee.dto.response.AttendeeResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.service.AttendeeInviteService;
import eventplanner.features.attendee.service.AttendeeService;
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
@Tag(name = "Attendee Invites", description = "Invite/accept/decline attendee access to events")
@SecurityRequirement(name = "bearerAuth")
public class AttendeeInviteController {

    private final AttendeeInviteService inviteService;
    private final AttendeeService attendeeService;
    private final AuthorizationService authorizationService;

    public AttendeeInviteController(
            AttendeeInviteService inviteService,
            AttendeeService attendeeService,
            AuthorizationService authorizationService
    ) {
        this.inviteService = inviteService;
        this.attendeeService = attendeeService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/api/v1/events/{eventId}/attendee-invites")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Create attendee invite", description = "Invite a user (by userId/email) to attend an event")
    public ResponseEntity<AttendeeInviteResponse> createInvite(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAttendeeInviteRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event");
        }
        try {
            AttendeeInviteResponse response = inviteService.createInvite(eventId, principal, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/api/v1/events/{eventId}/attendee-invites")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "List event attendee invites", description = "List pending/previous attendee invites for an event")
    public ResponseEntity<Page<AttendeeInviteResponse>> listEventInvites(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event");
        }
        return ResponseEntity.ok(inviteService.listEventInvites(eventId, page, size));
    }

    @DeleteMapping("/api/v1/events/{eventId}/attendee-invites/{inviteId}")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_DELETE, resources = {"event_id=#eventId"})
    @Operation(summary = "Revoke attendee invite", description = "Revoke a pending attendee invite")
    public ResponseEntity<Void> revokeInvite(
            @PathVariable UUID eventId,
            @PathVariable UUID inviteId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event");
        }
        try {
            inviteService.revokeInvite(inviteId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/api/v1/attendee-invites/incoming")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ)
    @Operation(summary = "List my attendee invites", description = "List pending attendee invites for the authenticated user")
    public ResponseEntity<List<AttendeeInviteResponse>> myInvites(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            return ResponseEntity.ok(inviteService.listMyPendingInvites(principal));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/api/v1/attendee-invites/{inviteId}/accept")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE, resources = {"invite_id=#inviteId"})
    @Operation(summary = "Accept attendee invite", description = "Accept an attendee invite (in-app)")
    public ResponseEntity<AttendeeResponse> acceptInvite(
            @PathVariable UUID inviteId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            Attendee attendee = inviteService.acceptInviteById(inviteId, principal);
            AttendeeResponse response = attendeeService.toResponse(attendee);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/api/v1/attendee-invites/accept")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE)
    @Operation(summary = "Accept attendee invite by token", description = "Accept an attendee invite using an email token (requires authentication)")
    public ResponseEntity<AttendeeResponse> acceptInviteByToken(
            @RequestParam String token,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            Attendee attendee = inviteService.acceptInviteByToken(token, principal);
            AttendeeResponse response = attendeeService.toResponse(attendee);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/api/v1/attendee-invites/{inviteId}/decline")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"invite_id=#inviteId"})
    @Operation(summary = "Decline attendee invite", description = "Decline an attendee invite")
    public ResponseEntity<Void> declineInvite(
            @PathVariable UUID inviteId,
            @AuthenticationPrincipal UserPrincipal principal
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
