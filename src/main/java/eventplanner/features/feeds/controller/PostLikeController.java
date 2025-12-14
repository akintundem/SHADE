package eventplanner.features.feeds.controller;

import eventplanner.features.feeds.service.PostLikeService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Post Likes", description = "Like/unlike feed posts")
@SecurityRequirement(name = "bearerAuth")
public class PostLikeController {

    private final PostLikeService likeService;

    public PostLikeController(PostLikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/{id}/posts/{postId}/like")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Like a post", description = "Like a feed post. Idempotent - if already liked, no error is thrown.")
    public ResponseEntity<Void> likePost(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        likeService.likePost(id, postId, principal);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/posts/{postId}/like")
    @RequiresPermission(value = RbacPermissions.EVENT_READ, resources = {"event_id=#id"})
    @Operation(summary = "Unlike a post", description = "Remove like from a feed post")
    public ResponseEntity<Void> unlikePost(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        likeService.unlikePost(id, postId, principal);
        return ResponseEntity.noContent().build();
    }
}
