package eventplanner.features.feeds.service;

import eventplanner.features.event.service.EventAccessControlService;
import eventplanner.features.feeds.dto.request.CommentCreateRequest;
import eventplanner.features.feeds.dto.request.CommentUpdateRequest;
import eventplanner.features.feeds.dto.response.CommentResponse;
import eventplanner.features.feeds.entity.EventFeedPost;
import eventplanner.features.feeds.entity.PostComment;
import eventplanner.features.feeds.repository.FeedPostRepository;
import eventplanner.features.feeds.repository.PostCommentRepository;
import eventplanner.common.storage.upload.MediaUploadStatus;
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
public class PostCommentService {

    private final PostCommentRepository commentRepository;
    private final FeedPostRepository postRepository;
    private final UserAccountRepository userAccountRepository;
    private final EventAccessControlService accessControlService;

    public PostCommentService(PostCommentRepository commentRepository,
                             FeedPostRepository postRepository,
                             UserAccountRepository userAccountRepository,
                             EventAccessControlService accessControlService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userAccountRepository = userAccountRepository;
        this.accessControlService = accessControlService;
    }

    public CommentResponse createComment(UUID eventId, UUID postId, UserPrincipal principal, CommentCreateRequest request) {
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

        String content = request != null ? safeTrimToNull(request.getContent()) : null;
        if (content == null) {
            throw new IllegalArgumentException("Comment content is required");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Comment content too long (max 2000 characters)");
        }

        UUID userId = principal.getId();
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        
        PostComment saved = commentRepository.save(comment);
        
        log.debug("User {} created comment {} on post {}", principal.getId(), saved.getId(), postId);
        
        return CommentResponse.from(saved);
    }

    public CommentResponse updateComment(UUID eventId, UUID postId, UUID commentId, UserPrincipal principal, CommentUpdateRequest request) {
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

        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        
        if (comment.getPost() == null || !postId.equals(comment.getPost().getId())) {
            throw new IllegalArgumentException("Comment does not belong to post");
        }

        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        // Only comment creator can update
        if (comment.getUser() == null || !comment.getUser().getId().equals(principal.getId())) {
            throw new IllegalArgumentException("Not authorized to update this comment");
        }

        String content = request != null ? safeTrimToNull(request.getContent()) : null;
        if (content == null) {
            throw new IllegalArgumentException("Comment content is required");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Comment content too long (max 2000 characters)");
        }

        comment.setContent(content);
        PostComment updated = commentRepository.save(comment);
        
        log.debug("User {} updated comment {}", principal.getId(), commentId);
        
        return CommentResponse.from(updated);
    }

    public void deleteComment(UUID eventId, UUID postId, UUID commentId, UserPrincipal principal) {
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

        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        
        if (comment.getPost() == null || !postId.equals(comment.getPost().getId())) {
            throw new IllegalArgumentException("Comment does not belong to post");
        }

        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        // Only comment creator can delete
        if (comment.getUser() == null || !comment.getUser().getId().equals(principal.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this comment");
        }

        commentRepository.delete(comment);
        
        log.debug("User {} deleted comment {}", principal.getId(), commentId);
    }

    public Page<CommentResponse> getComments(UUID eventId, UUID postId, UserPrincipal principal, Pageable pageable) {
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

        Page<PostComment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);
        return comments.map(CommentResponse::from);
    }

    public long getCommentCount(UUID postId) {
        return commentRepository.countByPostId(postId);
    }

    private String safeTrimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
