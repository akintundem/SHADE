package eventplanner.features.feeds.controller;

import eventplanner.features.feeds.dto.request.CommentCreateRequest;
import eventplanner.features.feeds.dto.request.CommentUpdateRequest;
import eventplanner.features.feeds.dto.request.FeedPostCreateRequest;
import eventplanner.features.feeds.dto.request.FeedPostMediaUploadCompleteRequest;
import eventplanner.features.feeds.dto.request.QuotePostRequest;
import eventplanner.features.feeds.dto.response.CommentResponse;
import eventplanner.features.feeds.dto.response.CreateFeedPostResponse;
import eventplanner.features.feeds.dto.response.FeedPostResponse;
import eventplanner.features.feeds.dto.response.PostListResponse;
import eventplanner.features.feeds.service.FeedPostService;
import eventplanner.features.feeds.service.PostCommentService;
import eventplanner.features.feeds.service.PostLikeService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Feed Posts", description = "Twitter-like event feed posts (immutable posts, media via presigned upload)")
@SecurityRequirement(name = "bearerAuth")
public class FeedPostController {

    private final FeedPostService postService;
    private final PostCommentService commentService;
    private final PostLikeService likeService;

    public FeedPostController(FeedPostService postService,
                             PostCommentService commentService,
                             PostLikeService likeService) {
        this.postService = postService;
        this.commentService = commentService;
        this.likeService = likeService;
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
    @Operation(summary = "List posts", description = "List posts for an event. Supports pagination via query parameters.")
    public ResponseEntity<?> list(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (max 100)") @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        // If pagination parameters are provided, use paginated endpoint
        if (page != null || size != null) {
            PostListResponse response = postService.listPaginated(id, principal, page, size);
            return ResponseEntity.ok(response);
        }
        // Otherwise, return all posts (backward compatibility)
        List<FeedPostResponse> posts = postService.list(id, principal);
        return ResponseEntity.ok(posts);
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

    @PostMapping("/{id}/posts/{postId}/repost")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Repost", description = "Repost a post to share it with your network")
    public ResponseEntity<FeedPostResponse> repost(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(postService.repost(id, postId, principal));
    }

    @PostMapping("/{id}/posts/{postId}/quote")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Quote post", description = "Repost with your own comment (quote post)")
    public ResponseEntity<FeedPostResponse> quotePost(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuotePostRequest request
    ) {
        return ResponseEntity.ok(postService.repost(id, postId, principal, request.getQuoteText()));
    }

    // ============================================================================
    // COMMENT ENDPOINTS
    // ============================================================================

    @PostMapping("/{id}/posts/{postId}/comments")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Create comment", description = "Add a comment to a post")
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity.ok(commentService.createComment(id, postId, principal, request));
    }

    @GetMapping("/{id}/posts/{postId}/comments")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Get comments", description = "Get all comments for a post with pagination")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(commentService.getComments(id, postId, principal, pageable));
    }

    @PutMapping("/{id}/posts/{postId}/comments/{commentId}")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Update comment", description = "Update a comment (creator only)")
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @Parameter(description = "Comment ID") @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ResponseEntity.ok(commentService.updateComment(id, postId, commentId, principal, request));
    }

    @DeleteMapping("/{id}/posts/{postId}/comments/{commentId}")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Delete comment", description = "Delete a comment (creator only)")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @Parameter(description = "Comment ID") @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        commentService.deleteComment(id, postId, commentId, principal);
        return ResponseEntity.noContent().build();
    }

    // ============================================================================
    // LIKE ENDPOINTS
    // ============================================================================

    @PostMapping("/{id}/posts/{postId}/like")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Like post", description = "Like a post")
    public ResponseEntity<Void> likePost(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        likeService.likePost(id, postId, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/posts/{postId}/like")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Unlike post", description = "Remove like from a post")
    public ResponseEntity<Void> unlikePost(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        likeService.unlikePost(id, postId, principal);
        return ResponseEntity.noContent().build();
    }
}


