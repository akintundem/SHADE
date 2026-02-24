package eventplanner.features.social.service;

import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.common.exception.exceptions.UnauthorizedException;
import eventplanner.features.social.dto.response.FollowStatsResponse;
import eventplanner.features.social.dto.response.FollowStatusResponse;
import eventplanner.features.social.dto.response.UserProfileResponse;
import eventplanner.features.social.entity.UserFollow;
import eventplanner.features.social.repository.UserFollowRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.ProfileImageService;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class UserFollowService {

    private final UserFollowRepository followRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProfileImageService profileImageService;

    public UserFollowService(UserFollowRepository followRepository,
                            UserAccountRepository userAccountRepository,
                            ProfileImageService profileImageService) {
        this.followRepository = followRepository;
        this.userAccountRepository = userAccountRepository;
        this.profileImageService = profileImageService;
    }

    /**
     * Follow a user
     */
    public void followUser(UUID followeeId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new UnauthorizedException("Authentication required");
        }

        UUID followerId = principal.getId();

        // Can't follow yourself
        if (followerId.equals(followeeId)) {
            throw new BadRequestException("Cannot follow yourself");
        }

        // Check if users exist
        UserAccount follower = userAccountRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserAccount followee = userAccountRepository.findById(followeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User to follow not found"));

        // Check if already following
        if (followRepository.existsActiveFollowByFollowerIdAndFolloweeId(followerId, followeeId)) {
            return; // Already following, idempotent operation
        }

        try {
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
        } catch (DataIntegrityViolationException e) {
            // Handle race condition: concurrent follow requests may cause unique constraint violation.
            // This is safe to ignore — the follow relationship already exists.
            log.debug("Duplicate follow attempted by user {} for user {} — already exists", followerId, followeeId);
        }
    }

    /**
     * Unfollow a user
     */
    public void unfollowUser(UUID followeeId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new UnauthorizedException("Authentication required");
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
        return toUserProfileResponsePage(follows.map(UserFollow::getFollowee), principal);
    }

    /**
     * Get followers of the specified user
     */
    public Page<UserProfileResponse> getFollowers(UUID userId, Pageable pageable, UserPrincipal principal) {
        Page<UserFollow> follows = followRepository.findActiveFollowersByUserId(userId, pageable);
        return toUserProfileResponsePage(follows.map(UserFollow::getFollower), principal);
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

    /**
     * Convert a page of UserAccounts to UserProfileResponses with batch-loaded follow statuses
     * to avoid N+1 queries.
     */
    private Page<UserProfileResponse> toUserProfileResponsePage(Page<UserAccount> users, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            return users.map(user -> buildProfileResponse(user, false, false));
        }

        UUID currentUserId = principal.getId();
        List<UUID> userIds = users.getContent().stream().map(UserAccount::getId).collect(Collectors.toList());

        // Batch load follow statuses: current user -> these users
        List<UUID> followingIds = followRepository.findActiveFolloweeIdsByFollowerIdAndFolloweeIdIn(currentUserId, userIds);
        // Batch load follow statuses: these users -> current user
        List<UUID> followedByIds = followRepository.findActiveFollowerIdsByFolloweeIdAndFollowerIdIn(currentUserId, userIds);

        Map<UUID, Boolean> isFollowingMap = userIds.stream()
                .collect(Collectors.toMap(id -> id, followingIds::contains));
        Map<UUID, Boolean> isFollowedByMap = userIds.stream()
                .collect(Collectors.toMap(id -> id, followedByIds::contains));

        return users.map(user -> {
            boolean isFollowing = isFollowingMap.getOrDefault(user.getId(), false);
            boolean isFollowedBy = isFollowedByMap.getOrDefault(user.getId(), false);
            return buildProfileResponse(user, isFollowing, isFollowedBy);
        });
    }

    private UserProfileResponse buildProfileResponse(UserAccount user, boolean isFollowing, boolean isFollowedBy) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setProfilePictureUrl(profileImageService.presignProfilePictureUrl(user.getProfilePictureUrl()));
        response.setIsFollowing(isFollowing);
        response.setIsFollowedBy(isFollowedBy);
        response.setIsMutual(isFollowing && isFollowedBy);
        return response;
    }
}
