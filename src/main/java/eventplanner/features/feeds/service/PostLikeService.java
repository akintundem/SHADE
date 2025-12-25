package eventplanner.features.feeds.service;

import eventplanner.features.event.service.EventAccessControlService;
import eventplanner.features.feeds.entity.EventFeedPost;
import eventplanner.features.feeds.entity.PostLike;
import eventplanner.features.feeds.repository.FeedPostRepository;
import eventplanner.features.feeds.repository.PostLikeRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@Transactional
public class PostLikeService {

    private final PostLikeRepository likeRepository;
    private final FeedPostRepository postRepository;
    private final UserAccountRepository userAccountRepository;
    private final EventAccessControlService accessControlService;

    public PostLikeService(PostLikeRepository likeRepository,
                          FeedPostRepository postRepository,
                          UserAccountRepository userAccountRepository,
                          EventAccessControlService accessControlService) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.userAccountRepository = userAccountRepository;
        this.accessControlService = accessControlService;
    }

    public void likePost(UUID eventId, UUID postId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        
        EventFeedPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        if (post.getEvent() == null || !eventId.equals(post.getEvent().getId())) {
            throw new IllegalArgumentException("Post not found");
        }

        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID userId = principal.getId();
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check if already liked
        if (likeRepository.existsByPostAndUser(post, user)) {
            return; // Already liked, no-op
        }

        PostLike like = new PostLike();
        like.setPost(post);
        like.setUser(user);
        likeRepository.save(like);
        
        log.debug("User {} liked post {}", userId, postId);
    }

    public void unlikePost(UUID eventId, UUID postId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        
        EventFeedPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        if (post.getEvent() == null || !eventId.equals(post.getEvent().getId())) {
            throw new IllegalArgumentException("Post not found");
        }

        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID userId = principal.getId();
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        likeRepository.deleteByPostAndUser(post, user);
        
        log.debug("User {} unliked post {}", userId, postId);
    }

    public boolean isLiked(UUID postId, UUID userId) {
        if (userId == null) {
            return false;
        }
        return likeRepository.existsByPostIdAndUserId(postId, userId);
    }

    public long getLikeCount(UUID postId) {
        return likeRepository.countByPostId(postId);
    }
}
