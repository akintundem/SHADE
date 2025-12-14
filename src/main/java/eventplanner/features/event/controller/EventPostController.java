package eventplanner.features.event.controller;

import eventplanner.features.event.dto.request.EventPostCreateRequest;
import eventplanner.features.event.dto.request.EventPostUpdateRequest;
import eventplanner.features.event.dto.response.EventPostResponse;
import eventplanner.features.event.service.EventPostService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Posts", description = "CRUD operations for event feed posts")
@SecurityRequirement(name = "bearerAuth")
public class EventPostController {

    private final EventPostService postService;

    public EventPostController(EventPostService postService) {
        this.postService = postService;
    }

    @PostMapping("/{id}/posts")
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#id"})
    @Operation(summary = "Create post", description = "Create a post for an event. For IMAGE/VIDEO posts, provide mediaObjectId (from the media upload flow).")
    public ResponseEntity<EventPostResponse> create(
        @Parameter(description = "Event ID") @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody EventPostCreateRequest request
    ) {
        return ResponseEntity.ok(postService.create(id, principal, request));
    }

    @GetMapping("/{id}/posts")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "List posts", description = "List posts for an event")
    public ResponseEntity<List<EventPostResponse>> list(
        @Parameter(description = "Event ID") @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(postService.list(id, principal));
    }

    @GetMapping("/{id}/posts/{postId}")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Get post", description = "Get a specific post")
    public ResponseEntity<EventPostResponse> get(
        @Parameter(description = "Event ID") @PathVariable UUID id,
        @Parameter(description = "Post ID") @PathVariable UUID postId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(postService.get(id, postId, principal));
    }

    @PutMapping("/{id}/posts/{postId}")
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#id"})
    @Operation(summary = "Update post", description = "Update a post's content or media reference")
    public ResponseEntity<EventPostResponse> update(
        @Parameter(description = "Event ID") @PathVariable UUID id,
        @Parameter(description = "Post ID") @PathVariable UUID postId,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody EventPostUpdateRequest request
    ) {
        return ResponseEntity.ok(postService.update(id, postId, principal, request));
    }

    @DeleteMapping("/{id}/posts/{postId}")
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#id"})
    @Operation(summary = "Delete post", description = "Delete a post")
    public ResponseEntity<Void> delete(
        @Parameter(description = "Event ID") @PathVariable UUID id,
        @Parameter(description = "Post ID") @PathVariable UUID postId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        postService.delete(id, postId, principal);
        return ResponseEntity.noContent().build();
    }
}


