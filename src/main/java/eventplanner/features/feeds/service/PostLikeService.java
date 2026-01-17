package eventplanner.features.feeds.service;

import eventplanner.features.event.service.EventAccessControlService;
import eventplanner.features.feeds.entity.EventFeedPost;
import eventplanner.features.feeds.entity.PostLike;
import eventplanner.features.feeds.repository.FeedPostRepository;
import eventplanner.features.feeds.repository.PostLikeRepository;
import eventplanner.common.storage.s3.dto.MediaUploadStatus;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.HashMap;

@Service
@Slf4j
@Transactional
public class PostLikeService {

    private final PostLikeRepository likeRepository;
    private final FeedPostRepository postRepository;
    private final UserAccountRepository userAccountRepository;
    private final EventAccessControlService accessControlService;
    private final NotificationService notificationService;

    public PostLikeService(PostLikeRepository likeRepository,
                          FeedPostRepository postRepository,
                          UserAccountRepository userAccountRepository,
                          EventAccessControlService accessControlService,
                          NotificationService notificationService) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.userAccountRepository = userAccountRepository;
        this.accessControlService = accessControlService;
        this.notificationService = notificationService;
    }

    public void likePost(UUID eventId, UUID postId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        
        EventFeedPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        if (post.getEvent() == null || !eventId.equals(post.getEvent().getId())) {
            throw new IllegalArgumentException("Post not found");
        }
        MediaUploadStatus status = post.getMediaUploadStatus();
        if (status != null && status != MediaUploadStatus.COMPLETED) {
            throw new IllegalArgumentException("Post not available");
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
        sendPostOwnerPush(post, user, "New like on your post");
    }

    public void unlikePost(UUID eventId, UUID postId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        
        EventFeedPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        if (post.getEvent() == null || !eventId.equals(post.getEvent().getId())) {
            throw new IllegalArgumentException("Post not found");
        }
        MediaUploadStatus status = post.getMediaUploadStatus();
        if (status != null && status != MediaUploadStatus.COMPLETED) {
            throw new IllegalArgumentException("Post not available");
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
    
    private void sendPostOwnerPush(EventFeedPost post, UserAccount actor, String subject) {
        try {
            if (post == null || post.getCreatedBy() == null || post.getCreatedBy().getId() == null) {
                return;
            }
            if (actor != null && actor.getId() != null && actor.getId().equals(post.getCreatedBy().getId())) {
                return; // Skip notifying self
            }
            UUID eventId = post.getEvent() != null ? post.getEvent().getId() : null;
            HashMap<String, Object> data = new HashMap<>();
            data.put("body", "Your post has a new like");
            data.put("postId", post.getId() != null ? post.getId().toString() : null);
            if (eventId != null) {
                data.put("eventId", eventId.toString());
            }
            
            notificationService.send(NotificationRequest.builder()
                    .type(CommunicationType.PUSH_NOTIFICATION)
                    .to(post.getCreatedBy().getId().toString())
                    .subject(subject)
                    .templateVariables(data)
                    .eventId(eventId)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send post like push for post {}: {}", post != null ? post.getId() : null, e.getMessage());
        }
    }
}
