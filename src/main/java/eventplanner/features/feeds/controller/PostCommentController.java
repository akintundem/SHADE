package eventplanner.features.feeds.controller;

import eventplanner.features.feeds.dto.request.CommentCreateRequest;
import eventplanner.features.feeds.dto.request.CommentUpdateRequest;
import eventplanner.features.feeds.dto.response.CommentResponse;
import eventplanner.features.feeds.service.PostCommentService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Post Comments", description = "Comment on feed posts")
@SecurityRequirement(name = "bearerAuth")
public class PostCommentController {

    private final PostCommentService commentService;

    public PostCommentController(PostCommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/{id}/posts/{postId}/comments")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Create comment", description = "Create a comment on a feed post")
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity.ok(commentService.createComment(id, postId, principal, request));
    }

    @PutMapping("/{id}/posts/{postId}/comments/{commentId}")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Update comment", description = "Update a comment (only by the comment creator)")
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
    @Operation(summary = "Delete comment", description = "Delete a comment (only by the comment creator)")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @Parameter(description = "Comment ID") @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        commentService.deleteComment(id, postId, commentId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/posts/{postId}/comments")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "List comments", description = "Get paginated list of comments for a post")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(commentService.getComments(id, postId, principal, pageable));
    }
}
