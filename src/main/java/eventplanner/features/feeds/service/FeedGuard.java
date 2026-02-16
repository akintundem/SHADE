package eventplanner.features.feeds.service;

import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.common.storage.s3.dto.MediaUploadStatus;
import eventplanner.features.feeds.entity.EventFeedPost;

/**
 * Shared guard checks used by PostCommentService, PostLikeService, and FeedPostService.
 * Eliminates 6+ copies of the same media-status validation.
 */
public final class FeedGuard {

    private FeedGuard() {}

    /**
     * Ensure a feed post's media upload is complete (or has no media).
     * Throws if the post is still uploading.
     */
    public static void ensurePostAvailable(EventFeedPost post) {
        if (post == null) {
            throw new ResourceNotFoundException("Post not found");
        }
        MediaUploadStatus status = post.getMediaUploadStatus();
        if (status != null && status != MediaUploadStatus.COMPLETED) {
            throw new IllegalArgumentException("Post not available");
        }
    }
}
