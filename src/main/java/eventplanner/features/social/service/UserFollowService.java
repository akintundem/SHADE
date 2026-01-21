package eventplanner.features.social.service;

import eventplanner.features.social.dto.response.FollowStatsResponse;
import eventplanner.features.social.dto.response.FollowStatusResponse;
import eventplanner.features.social.dto.response.UserProfileResponse;
import eventplanner.features.social.entity.UserFollow;
import eventplanner.features.social.repository.UserFollowRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@Transactional
public class UserFollowService {

    private final UserFollowRepository followRepository;
    private final UserAccountRepository userAccountRepository;

    public UserFollowService(UserFollowRepository followRepository,
                            UserAccountRepository userAccountRepository) {
        this.followRepository = followRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Follow a user
     */
    public void followUser(UUID followeeId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID followerId = principal.getId();

        // Can't follow yourself
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }

        // Check if users exist
        UserAccount follower = userAccountRepository.findById(followerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserAccount followee = userAccountRepository.findById(followeeId)
                .orElseThrow(() -> new IllegalArgumentException("User to follow not found"));

        // Check if already following
        if (followRepository.existsActiveFollowByFollowerIdAndFolloweeId(followerId, followeeId)) {
            return; // Already following, idempotent operation
        }

        // Check if a follow record already exists (could be BLOCKED or PENDING)
        followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .ifPresentOrElse(
                        existingFollow -> {
                            // Reactivate follow if it was blocked or pending
                            existingFollow.setStatus(UserFollow.FollowStatus.ACTIVE);
                            followRepository.save(existingFollow);
                            log.info("User {} reactivated follow for user {}", followerId, followeeId);
                        },
                        () -> {
                            // Create new follow relationship
                            UserFollow follow = new UserFollow();
                            follow.setFollower(follower);
                            follow.setFollowee(followee);
                            follow.setStatus(UserFollow.FollowStatus.ACTIVE);
                            followRepository.save(follow);
                            log.info("User {} started following user {}", followerId, followeeId);
                        }
                );
    }

    /**
     * Unfollow a user
     */
    public void unfollowUser(UUID followeeId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID followerId = principal.getId();

        // Find and delete follow relationship
        followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .ifPresent(follow -> {
                    followRepository.delete(follow);
                    log.info("User {} unfollowed user {}", followerId, followeeId);
                });
    }

    /**
     * Get follow status between current user and another user
     */
    public FollowStatusResponse getFollowStatus(UUID userId, UserPrincipal principal) {
        FollowStatusResponse response = new FollowStatusResponse();
        response.setUserId(userId);

        if (principal == null || principal.getId() == null) {
            response.setIsFollowing(false);
            response.setIsFollowedBy(false);
            response.setIsMutual(false);
            return response;
        }

        UUID currentUserId = principal.getId();

        // Check if current user follows target user
        boolean isFollowing = followRepository.existsActiveFollowByFollowerIdAndFolloweeId(
                currentUserId, userId);

        // Check if target user follows current user
        boolean isFollowedBy = followRepository.existsActiveFollowByFollowerIdAndFolloweeId(
                userId, currentUserId);

        response.setIsFollowing(isFollowing);
        response.setIsFollowedBy(isFollowedBy);
        response.setIsMutual(isFollowing && isFollowedBy);

        return response;
    }

    /**
     * Get users that the specified user is following
     */
    public Page<UserProfileResponse> getFollowing(UUID userId, Pageable pageable, UserPrincipal principal) {
        Page<UserFollow> follows = followRepository.findActiveFollowingByUserId(userId, pageable);
        return follows.map(follow -> toUserProfileResponse(follow.getFollowee(), principal));
    }

    /**
     * Get followers of the specified user
     */
    public Page<UserProfileResponse> getFollowers(UUID userId, Pageable pageable, UserPrincipal principal) {
        Page<UserFollow> follows = followRepository.findActiveFollowersByUserId(userId, pageable);
        return follows.map(follow -> toUserProfileResponse(follow.getFollower(), principal));
    }

    /**
     * Get follow statistics for a user
     */
    public FollowStatsResponse getFollowStats(UUID userId) {
        long followingCount = followRepository.countActiveFollowingByUserId(userId);
        long followersCount = followRepository.countActiveFollowersByUserId(userId);

        FollowStatsResponse stats = new FollowStatsResponse();
        stats.setUserId(userId);
        stats.setFollowingCount(followingCount);
        stats.setFollowersCount(followersCount);

        return stats;
    }

    /**
     * Check if user is following another user
     */
    public boolean isFollowing(UUID followerId, UUID followeeId) {
        return followRepository.existsActiveFollowByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    /**
     * Check if two users are mutual followers
     */
    public boolean areMutualFollowers(UUID userId1, UUID userId2) {
        return followRepository.areMutualFollowers(userId1, userId2);
    }

    private UserProfileResponse toUserProfileResponse(UserAccount user, UserPrincipal principal) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setProfilePictureUrl(user.getProfilePictureUrl());

        // Add follow context if user is authenticated
        if (principal != null && principal.getId() != null) {
            boolean isFollowing = followRepository.existsActiveFollowByFollowerIdAndFolloweeId(
                    principal.getId(), user.getId());
            boolean isFollowedBy = followRepository.existsActiveFollowByFollowerIdAndFolloweeId(
                    user.getId(), principal.getId());

            response.setIsFollowing(isFollowing);
            response.setIsFollowedBy(isFollowedBy);
            response.setIsMutual(isFollowing && isFollowedBy);
        }

        return response;
    }
}
