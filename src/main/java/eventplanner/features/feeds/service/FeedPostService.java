package eventplanner.features.feeds.service;

import eventplanner.common.storage.s3.services.S3StorageService;
import eventplanner.common.storage.upload.MediaUploadStatus;
import eventplanner.common.storage.upload.PresignedUploadCompleteRequest;
import eventplanner.common.storage.upload.PresignedUploadRequest;
import eventplanner.common.storage.upload.PresignedUploadService;
import eventplanner.common.storage.upload.UploadCompletionCallback;
import eventplanner.features.event.entity.EventStoredObject;
import eventplanner.features.event.repository.EventStoredObjectRepository;
import eventplanner.features.event.service.EventAccessControlService;
import eventplanner.features.feeds.dto.request.FeedPostCreateRequest;
import eventplanner.features.feeds.dto.request.FeedPostMediaUploadCompleteRequest;
import eventplanner.features.feeds.dto.request.FeedPostMediaUploadRequest;
import eventplanner.features.feeds.dto.response.CreateFeedPostResponse;
import eventplanner.features.feeds.dto.response.FeedPostResponse;
import eventplanner.features.feeds.dto.response.PresignedUploadResponse;
import eventplanner.features.feeds.entity.EventFeedPost;
import eventplanner.features.feeds.repository.FeedPostRepository;
import eventplanner.security.auth.service.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FeedPostService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final String EVENT_BUCKET_ALIAS = "event";
    private static final String PURPOSE_POST_MEDIA = "post_media";

    private final EventAccessControlService accessControlService;
    private final FeedPostRepository postRepository;
    private final EventStoredObjectRepository storedObjectRepository;
    private final S3StorageService storageService;
    private final PresignedUploadService presignedUploadService;

    public FeedPostService(EventAccessControlService accessControlService,
                           FeedPostRepository postRepository,
                           EventStoredObjectRepository storedObjectRepository,
                           S3StorageService storageService,
                           PresignedUploadService presignedUploadService) {
        this.accessControlService = accessControlService;
        this.postRepository = postRepository;
        this.storedObjectRepository = storedObjectRepository;
        this.storageService = storageService;
        this.presignedUploadService = presignedUploadService;
    }

    /**
     * Create a post. For IMAGE/VIDEO posts, returns a presigned upload (client must upload to S3, then call complete).
     * Posts are intentionally immutable after creation (no edit endpoint).
     */
    public CreateFeedPostResponse create(UUID eventId, UserPrincipal principal, FeedPostCreateRequest request) {
        accessControlService.requireMediaUpload(principal, eventId);
        EventFeedPost.PostType type = parseType(request != null ? request.getType() : null);

        String content = request != null ? safeTrimToNull(request.getContent()) : null;
        if (type == EventFeedPost.PostType.TEXT) {
            if (!StringUtils.hasText(content)) {
                throw new IllegalArgumentException("content is required for TEXT posts");
            }
        }
        if (content != null && content.length() > 4000) {
            throw new IllegalArgumentException("content too long");
        }

        EventFeedPost post = new EventFeedPost();
        post.setEventId(eventId);
        post.setPostType(type);
        post.setContent(content);
        post.setCreatedBy(principal != null ? principal.getId() : null);
        
        // Set initial status: TEXT posts are complete, IMAGE/VIDEO start as PENDING
        if (type == EventFeedPost.PostType.TEXT) {
            post.setMediaUploadStatus(MediaUploadStatus.COMPLETED);
        } else {
            post.setMediaUploadStatus(MediaUploadStatus.PENDING);
        }

        PresignedUploadResponse upload = null;

        if (type == EventFeedPost.PostType.IMAGE || type == EventFeedPost.PostType.VIDEO) {
            FeedPostMediaUploadRequest mediaUpload = request != null ? request.getMediaUpload() : null;
            if (mediaUpload == null) {
                throw new IllegalArgumentException("mediaUpload is required for " + type);
            }
            validateMediaType(type, mediaUpload.getContentType());

            // Use generic presigned upload service
            PresignedUploadRequest uploadRequest = new PresignedUploadRequest();
            uploadRequest.setFileName(mediaUpload.getFileName());
            uploadRequest.setContentType(mediaUpload.getContentType());
            uploadRequest.setCategory("post");
            uploadRequest.setIsPublic(mediaUpload.getIsPublic());
            uploadRequest.setDescription(mediaUpload.getDescription());
            
            eventplanner.common.storage.upload.PresignedUploadResponse genericResponse = presignedUploadService.generatePresignedUpload(
                    uploadRequest,
                    mediaId -> buildObjectKey(eventId, mediaId),
                    EVENT_BUCKET_ALIAS,
                    UPLOAD_URL_TTL
            );

            post.setMediaObjectId(genericResponse.getMediaId());
            EventFeedPost saved = postRepository.save(post);

            // Convert to feed-specific response DTO (they have the same structure)
            upload = PresignedUploadResponse.builder()
                    .mediaId(genericResponse.getMediaId())
                    .objectKey(genericResponse.getObjectKey())
                    .uploadMethod(genericResponse.getUploadMethod())
                    .uploadUrl(genericResponse.getUploadUrl())
                    .headers(genericResponse.getHeaders())
                    .resourceUrl(genericResponse.getResourceUrl())
                    .expiresAt(genericResponse.getExpiresAt())
                    .build();

            return CreateFeedPostResponse.builder()
                    .post(toResponse(saved, eventId))
                    .mediaUpload(upload)
                    .build();
        }

        EventFeedPost saved = postRepository.save(post);
        return CreateFeedPostResponse.builder()
                .post(toResponse(saved, eventId))
                .mediaUpload(null)
                .build();
    }

    /**
     * Complete the post media upload. Must match the post's mediaObjectId.
     */
    public FeedPostResponse completeMediaUpload(UUID eventId,
                                                UUID postId,
                                                UUID mediaId,
                                                UserPrincipal principal,
                                                FeedPostMediaUploadCompleteRequest request) {
        accessControlService.requireMediaUpload(principal, eventId);
        EventFeedPost post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (!eventId.equals(post.getEventId())) {
            throw new IllegalArgumentException("Post not found");
        }
        if (post.getMediaObjectId() == null || !post.getMediaObjectId().equals(mediaId)) {
            throw new IllegalArgumentException("Media does not belong to post");
        }

        // Only creator (or event owner/manager via access control checks) should be able to complete.
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (post.getCreatedBy() != null && !post.getCreatedBy().equals(principal.getId())) {
            // fallback to media-manage for privileged roles
            accessControlService.requireMediaManage(principal, eventId);
        }

        String expectedKey = buildObjectKey(eventId, mediaId);
        
        // Use generic presigned upload service
        PresignedUploadCompleteRequest completeRequest = new PresignedUploadCompleteRequest();
        completeRequest.setObjectKey(request.getObjectKey());
        completeRequest.setResourceUrl(request.getResourceUrl());
        completeRequest.setFileName(request.getFileName());
        completeRequest.setContentType(request.getContentType());
        completeRequest.setCategory("post");
        completeRequest.setIsPublic(request.getIsPublic());
        completeRequest.setDescription(request.getDescription());
        completeRequest.setTags(request.getTags());
        completeRequest.setMetadata(request.getMetadata());
        
        // Callback to update post status to COMPLETED
        UploadCompletionCallback callback = (entityId, completedMediaId, user) -> {
            post.setMediaUploadStatus(MediaUploadStatus.COMPLETED);
            postRepository.save(post);
        };
        
        presignedUploadService.completeUpload(
                eventId,
                mediaId,
                postId,
                PURPOSE_POST_MEDIA,
                completeRequest,
                expectedKey,
                principal,
                callback
        );

        return toResponse(post, eventId);
    }

    public List<FeedPostResponse> list(UUID eventId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        // Get all posts for the event, then filter to only show completed ones
        // This includes posts with null status (existing posts before status tracking)
        return postRepository.findByEventIdOrderByCreatedAtDesc(eventId)
            .stream()
            .filter(p -> {
                MediaUploadStatus status = p.getMediaUploadStatus();
                // Include COMPLETED or null (null = existing posts, treat as completed)
                return status == null || status == MediaUploadStatus.COMPLETED;
            })
            .map(p -> toResponse(p, eventId))
            .collect(Collectors.toList());
    }

    public FeedPostResponse get(UUID eventId, UUID postId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        EventFeedPost post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (!eventId.equals(post.getEventId())) {
            throw new IllegalArgumentException("Post not found");
        }
        // Only return if completed (null status = existing post, treat as completed)
        // Or allow creator to see their own pending posts
        MediaUploadStatus status = post.getMediaUploadStatus();
        if (status != null && status != MediaUploadStatus.COMPLETED) {
            if (principal == null || !post.getCreatedBy().equals(principal.getId())) {
                throw new IllegalArgumentException("Post not found");
            }
        }
        return toResponse(post, eventId);
    }

    public void delete(UUID eventId, UUID postId, UserPrincipal principal) {
        accessControlService.requireMediaUpload(principal, eventId);
        EventFeedPost post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (!eventId.equals(post.getEventId())) {
            throw new IllegalArgumentException("Post not found");
        }

        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        boolean isCreator = post.getCreatedBy() != null && post.getCreatedBy().equals(principal.getId());
        if (!isCreator) {
            accessControlService.requireMediaManage(principal, eventId);
        }

        postRepository.delete(post);
    }

    private FeedPostResponse toResponse(EventFeedPost post, UUID eventId) {
        FeedPostResponse resp = new FeedPostResponse();
        resp.setId(post.getId());
        resp.setEventId(eventId);
        resp.setType(post.getPostType() != null ? post.getPostType().name() : "TEXT");
        resp.setContent(post.getContent());
        resp.setMediaObjectId(post.getMediaObjectId());
        resp.setCreatedBy(post.getCreatedBy());
        resp.setCreatedAt(post.getCreatedAt());
        resp.setUpdatedAt(post.getUpdatedAt());

        if (post.getMediaObjectId() != null) {
            EventStoredObject obj = storedObjectRepository.findById(post.getMediaObjectId()).orElse(null);
            if (obj != null && eventId.equals(obj.getEventId()) && PURPOSE_POST_MEDIA.equals(obj.getPurpose())) {
                URL presignedGet = storageService.generatePresignedGetUrl(EVENT_BUCKET_ALIAS, obj.getObjectKey(), DOWNLOAD_URL_TTL);
                resp.setMediaUrl(presignedGet.toString());
            }
        }
        return resp;
    }

    private EventFeedPost.PostType parseType(String raw) {
        if (!StringUtils.hasText(raw)) {
            return EventFeedPost.PostType.TEXT;
        }
        try {
            return EventFeedPost.PostType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid post type");
        }
    }

    private void validateMediaType(EventFeedPost.PostType type, String contentType) {
        if (!StringUtils.hasText(contentType)) {
            throw new IllegalArgumentException("contentType is required");
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (type == EventFeedPost.PostType.IMAGE && !ct.startsWith("image/")) {
            throw new IllegalArgumentException("IMAGE posts must use image/* contentType");
        }
        if (type == EventFeedPost.PostType.VIDEO && !ct.startsWith("video/")) {
            throw new IllegalArgumentException("VIDEO posts must use video/* contentType");
        }
    }

    private String buildObjectKey(UUID eventId, UUID mediaId) {
        return "events/" + eventId + "/" + PURPOSE_POST_MEDIA + "/" + mediaId;
    }

    private String safeTrimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}


