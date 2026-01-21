package eventplanner.features.social.controller;

import eventplanner.features.social.dto.response.FollowStatsResponse;
import eventplanner.features.social.dto.response.FollowStatusResponse;
import eventplanner.features.social.dto.response.UserProfileResponse;
import eventplanner.features.social.service.UserFollowService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Follows", description = "User follow/social graph functionality")
@SecurityRequirement(name = "bearerAuth")
public class UserFollowController {

    private final UserFollowService followService;

    public UserFollowController(UserFollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{userId}/follow")
    @RequiresPermission(RbacPermissions.USER_UPDATE)
    @Operation(summary = "Follow user", description = "Follow another user")
    public ResponseEntity<Void> followUser(
            @Parameter(description = "User ID to follow") @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        followService.followUser(userId, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/follow")
    @RequiresPermission(RbacPermissions.USER_UPDATE)
    @Operation(summary = "Unfollow user", description = "Unfollow a user")
    public ResponseEntity<Void> unfollowUser(
            @Parameter(description = "User ID to unfollow") @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        followService.unfollowUser(userId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/follow-status")
    @RequiresPermission(RbacPermissions.USER_READ)
    @Operation(summary = "Get follow status", description = "Get follow relationship status between current user and target user")
    public ResponseEntity<FollowStatusResponse> getFollowStatus(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(followService.getFollowStatus(userId, principal));
    }

    @GetMapping("/{userId}/following")
    @RequiresPermission(RbacPermissions.USER_READ)
    @Operation(summary = "Get following", description = "Get list of users that this user is following")
    public ResponseEntity<Page<UserProfileResponse>> getFollowing(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(followService.getFollowing(userId, pageable, principal));
    }

    @GetMapping("/{userId}/followers")
    @RequiresPermission(RbacPermissions.USER_READ)
    @Operation(summary = "Get followers", description = "Get list of users following this user")
    public ResponseEntity<Page<UserProfileResponse>> getFollowers(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(followService.getFollowers(userId, pageable, principal));
    }

    @GetMapping("/{userId}/follow-stats")
    @RequiresPermission(RbacPermissions.USER_READ)
    @Operation(summary = "Get follow stats", description = "Get follower and following counts for a user")
    public ResponseEntity<FollowStatsResponse> getFollowStats(
            @Parameter(description = "User ID") @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(followService.getFollowStats(userId));
    }
}
