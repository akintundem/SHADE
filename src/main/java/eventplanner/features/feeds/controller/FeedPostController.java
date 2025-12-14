package eventplanner.features.feeds.controller;

import eventplanner.features.feeds.dto.request.FeedPostCreateRequest;
import eventplanner.features.feeds.dto.request.FeedPostMediaUploadCompleteRequest;
import eventplanner.features.feeds.dto.response.CreateFeedPostResponse;
import eventplanner.features.feeds.dto.response.FeedPostResponse;
import eventplanner.features.feeds.service.FeedPostService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Feed Posts", description = "Twitter-like event feed posts (immutable posts, media via presigned upload)")
@SecurityRequirement(name = "bearerAuth")
public class FeedPostController {

    private final FeedPostService postService;

    public FeedPostController(FeedPostService postService) {
        this.postService = postService;
    }

    @PostMapping("/{id}/posts")
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#id"})
    @Operation(summary = "Create post", description = "Create a post for an event. For IMAGE/VIDEO posts, the API returns a presigned upload; upload to S3 then call the complete endpoint. Posts cannot be edited.")
    public ResponseEntity<CreateFeedPostResponse> create(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FeedPostCreateRequest request
    ) {
        return ResponseEntity.ok(postService.create(id, principal, request));
    }

    @PostMapping("/{id}/posts/{postId}/media/{mediaId}/complete")
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#id"})
    @Operation(summary = "Complete post media upload", description = "Called after the client uploads to S3 using the presigned URL returned by create(). Persists media metadata and makes it available on the post.")
    public ResponseEntity<FeedPostResponse> completeMediaUpload(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @Parameter(description = "Media ID") @PathVariable UUID mediaId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FeedPostMediaUploadCompleteRequest request
    ) {
        return ResponseEntity.ok(postService.completeMediaUpload(id, postId, mediaId, principal, request));
    }

    @GetMapping("/{id}/posts")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "List posts", description = "List posts for an event")
    public ResponseEntity<List<FeedPostResponse>> list(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(postService.list(id, principal));
    }

    @GetMapping("/{id}/posts/{postId}")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Get post", description = "Get a specific post")
    public ResponseEntity<FeedPostResponse> get(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(postService.get(id, postId, principal));
    }

    @DeleteMapping("/{id}/posts/{postId}")
    @RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#id"})
    @Operation(summary = "Delete post", description = "Delete a post (creator or event media-managers)")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        postService.delete(id, postId, principal);
        return ResponseEntity.noContent().build();
    }
}


