package eventplanner.features.feeds.service;

import eventplanner.common.storage.s3.services.S3StorageService;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    public FeedPostService(EventAccessControlService accessControlService,
                           FeedPostRepository postRepository,
                           EventStoredObjectRepository storedObjectRepository,
                           S3StorageService storageService) {
        this.accessControlService = accessControlService;
        this.postRepository = postRepository;
        this.storedObjectRepository = storedObjectRepository;
        this.storageService = storageService;
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

        PresignedUploadResponse upload = null;

        if (type == EventFeedPost.PostType.IMAGE || type == EventFeedPost.PostType.VIDEO) {
            FeedPostMediaUploadRequest mediaUpload = request != null ? request.getMediaUpload() : null;
            if (mediaUpload == null) {
                throw new IllegalArgumentException("mediaUpload is required for " + type);
            }
            ensureS3Configured();
            validateMediaType(type, mediaUpload.getContentType());

            UUID mediaId = UUID.randomUUID();
            String objectKey = buildObjectKey(eventId, mediaId);
            URL presignedPut = storageService.generatePresignedPutUrl(EVENT_BUCKET_ALIAS, objectKey, UPLOAD_URL_TTL, mediaUpload.getContentType());

            post.setMediaObjectId(mediaId); // media will be completed later
            EventFeedPost saved = postRepository.save(post);

            upload = PresignedUploadResponse.builder()
                    .mediaId(mediaId)
                    .objectKey(objectKey)
                    .uploadMethod("PUT")
                    .uploadUrl(presignedPut.toString())
                    .headers(Map.of("Content-Type", mediaUpload.getContentType()))
                    .resourceUrl(storageService.stripQuery(presignedPut))
                    .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(UPLOAD_URL_TTL))
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
        if (request == null || !StringUtils.hasText(request.getObjectKey())) {
            throw new IllegalArgumentException("objectKey is required");
        }
        if (!expectedKey.equals(request.getObjectKey())) {
            throw new IllegalArgumentException("objectKey does not match expected key for this upload");
        }

        EventStoredObject item = storedObjectRepository.findById(mediaId).orElseGet(EventStoredObject::new);
        item.setId(mediaId);
        item.setEventId(eventId);
        item.setPurpose(PURPOSE_POST_MEDIA);
        item.setOwnerId(postId);
        item.setObjectKey(expectedKey);
        item.setResourceUrl(safeTrimToNull(request.getResourceUrl()));
        item.setFileName(request.getFileName());
        item.setContentType(request.getContentType());
        item.setCategory("post");
        item.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        item.setDescription(safeTrimToNull(request.getDescription()));
        item.setTags(safeTrimToNull(request.getTags()));
        item.setMetadata(safeTrimToNull(request.getMetadata()));
        item.setUploadedBy(principal.getId());
        storedObjectRepository.save(item);

        // Ensure post points to the media id (already should)
        post.setMediaObjectId(mediaId);
        postRepository.save(post);

        return toResponse(post, eventId);
    }

    public List<FeedPostResponse> list(UUID eventId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        return postRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .map(p -> toResponse(p, eventId))
                .collect(Collectors.toList());
    }

    public FeedPostResponse get(UUID eventId, UUID postId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        EventFeedPost post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (!eventId.equals(post.getEventId())) {
            throw new IllegalArgumentException("Post not found");
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

    private void ensureS3Configured() {
        if (!storageService.isConfigured()) {
            throw new IllegalArgumentException("S3 is not configured for uploads");
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


