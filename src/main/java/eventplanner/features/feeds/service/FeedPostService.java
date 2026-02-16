package eventplanner.features.feeds.service;

import eventplanner.common.storage.s3.registry.BucketAlias;
import eventplanner.common.storage.s3.services.S3StorageService;
import eventplanner.common.storage.s3.dto.MediaUploadStatus;
import eventplanner.common.storage.s3.dto.PresignedUploadCompleteRequest;
import eventplanner.common.storage.s3.dto.PresignedUploadRequest;
import eventplanner.common.storage.s3.services.PresignedUploadService;
import eventplanner.common.storage.s3.UploadCompletionCallback;
import eventplanner.common.util.UserAccountUtil;
import eventplanner.common.exception.exceptions.ForbiddenException;
import eventplanner.common.exception.exceptions.UnauthorizedException;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventStoredObject;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.event.repository.EventStoredObjectRepository;
import eventplanner.features.event.service.EventAccessControlService;
import eventplanner.features.feeds.dto.request.FeedPostCreateRequest;
import eventplanner.features.feeds.dto.request.FeedPostMediaUploadCompleteRequest;
import eventplanner.features.feeds.dto.request.FeedPostMediaUploadRequest;
import eventplanner.features.feeds.dto.response.CreateFeedPostResponse;
import eventplanner.features.feeds.dto.response.FeedPostResponse;
import eventplanner.features.feeds.dto.response.PostListResponse;
import eventplanner.features.feeds.entity.EventFeedPost;
import eventplanner.features.feeds.repository.FeedPostRepository;
import eventplanner.features.config.FeedsCleanupProperties;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@Slf4j
@Transactional
public class FeedPostService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final BucketAlias EVENT_BUCKET_ALIAS = BucketAlias.EVENT;
    private static final String PURPOSE_POST_MEDIA = "post_media";

    private final EventAccessControlService accessControlService;
    private final EventRepository eventRepository;
    private final FeedPostRepository postRepository;
    private final EventStoredObjectRepository storedObjectRepository;
    private final S3StorageService storageService;
    private final PresignedUploadService presignedUploadService;
    private final UserAccountRepository userAccountRepository;
    private final PostLikeService likeService;
    private final PostCommentService commentService;

    private final int maxAgeMinutes;

    public FeedPostService(EventAccessControlService accessControlService,
                           EventRepository eventRepository,
                           FeedPostRepository postRepository,
                           EventStoredObjectRepository storedObjectRepository,
                           S3StorageService storageService,
                           PresignedUploadService presignedUploadService,
                           UserAccountRepository userAccountRepository,
                           PostLikeService likeService,
                           PostCommentService commentService,
                           FeedsCleanupProperties feedsCleanupProperties) {
        this.accessControlService = accessControlService;
        this.eventRepository = eventRepository;
        this.postRepository = postRepository;
        this.storedObjectRepository = storedObjectRepository;
        this.storageService = storageService;
        this.presignedUploadService = presignedUploadService;
        this.userAccountRepository = userAccountRepository;
        this.likeService = likeService;
        this.commentService = commentService;
        this.maxAgeMinutes = requireConfigured(feedsCleanupProperties.getMaxAgeMinutes(), "feeds.cleanup.max-age-minutes");
    }

    /**
     * Create a post. For IMAGE/VIDEO posts, returns a presigned upload (client must upload to S3, then call complete).
     * Posts are intentionally immutable after creation (no edit endpoint).
     */
    public CreateFeedPostResponse create(UUID eventId, UserPrincipal principal, FeedPostCreateRequest request) {
        accessControlService.requireMediaUpload(principal, eventId);
        
        // Prevent feed post creation for archived or cancelled events
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (Boolean.TRUE.equals(event.getIsArchived())) {
            throw new IllegalArgumentException("Cannot create feed posts for archived events");
        }
        if (event.getEventStatus() == eventplanner.features.event.enums.EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot create feed posts for cancelled events");
        }
        
        // Note: For TICKETED events, accessControlService.requireMediaUpload() already validates
        // that the user has proper access (ticket/ownership/collaborator status).
        // This check is redundant but ensures we don't create posts for cancelled events.
        
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
        post.setEvent(event);
        post.setPostType(type);
        post.setContent(content);
        
        // Set createdBy relationship using managed entity
        UserAccount creator = (principal != null && principal.getId() != null) 
            ? UserAccountUtil.getManagedUserAccountOrThrow(principal, userAccountRepository, "User not found")
            : null;
        post.setCreatedBy(creator);
        
        // Set initial status: TEXT posts are complete, IMAGE/VIDEO start as PENDING
        if (type == EventFeedPost.PostType.TEXT) {
            post.setMediaUploadStatus(MediaUploadStatus.COMPLETED);
        } else {
            post.setMediaUploadStatus(MediaUploadStatus.PENDING);
        }

        eventplanner.common.storage.s3.dto.PresignedUploadResponse upload = null;

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

            upload = presignedUploadService.generatePresignedUpload(
                    uploadRequest,
                    mediaId -> buildObjectKey(event.getId(), mediaId),
                    EVENT_BUCKET_ALIAS,
                    UPLOAD_URL_TTL
            );

            post.setMediaObjectId(upload.getMediaId());
            EventFeedPost saved = postRepository.save(post);

            return CreateFeedPostResponse.builder()
                    .post(enrichPostsWithEngagement(List.of(saved), eventId, principal).get(0))
                    .mediaUpload(upload)
                    .build();
        }

        EventFeedPost saved = postRepository.save(post);
        return CreateFeedPostResponse.builder()
                .post(enrichPostsWithEngagement(List.of(saved), eventId, principal).get(0))
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
        if (post.getEvent() == null || !eventId.equals(post.getEvent().getId())) {
            throw new IllegalArgumentException("Post not found");
        }
        if (post.getMediaObjectId() == null || !post.getMediaObjectId().equals(mediaId)) {
            throw new IllegalArgumentException("Media does not belong to post");
        }
        if (post.getMediaUploadStatus() == MediaUploadStatus.COMPLETED) {
            // Idempotent: already completed, just return current view
            return enrichPostsWithEngagement(List.of(post), eventId, principal).get(0);
        }

        // Creator completes the upload; privileged roles can also finish after access control checks.
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (post.getCreatedBy() != null && !post.getCreatedBy().getId().equals(principal.getId())) {
            // fallback to media-manage for privileged roles
            accessControlService.requireMediaManage(principal, eventId);
        }

        String expectedKey = buildObjectKey(eventId, mediaId);
        if (request.getObjectKey() != null && !expectedKey.equals(request.getObjectKey())) {
            throw new IllegalArgumentException("Invalid object key for post media");
        }
        
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
                EVENT_BUCKET_ALIAS,
                principal,
                callback
        );

        // Reload post to get updated status
        EventFeedPost updatedPost = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        return enrichPostsWithEngagement(List.of(updatedPost), eventId, principal).get(0);
    }

    public List<FeedPostResponse> list(UUID eventId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        
        // Check if event is archived - don't show feed posts for archived events
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (Boolean.TRUE.equals(event.getIsArchived())) {
            return List.of(); // Return empty list for archived events
        }
        
        // Get all posts for the event, then filter to only show completed ones
        // This includes posts with null status (existing posts before status tracking)
        List<EventFeedPost> posts = postRepository.findByEventIdOrderByCreatedAtDesc(eventId)
            .stream()
            .filter(p -> {
                MediaUploadStatus status = p.getMediaUploadStatus();
                // Include COMPLETED or null (null = existing posts, treat as completed)
                return status == null || status == MediaUploadStatus.COMPLETED;
            })
            .collect(Collectors.toList());
        
        return enrichPostsWithEngagement(posts, eventId, principal);
    }

    public PostListResponse listPaginated(UUID eventId, UserPrincipal principal, Integer page, Integer size) {
        accessControlService.requireMediaView(principal, eventId);
        
        // Check if event is archived - don't show feed posts for archived events
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (Boolean.TRUE.equals(event.getIsArchived())) {
            PostListResponse response = new PostListResponse();
            response.setPosts(List.of());
            response.setCurrentPage(page != null ? page : 0);
            response.setPageSize(size != null ? size : 20);
            response.setTotalPosts(0L);
            response.setTotalPages(0);
            response.setHasNext(false);
            response.setHasPrevious(false);
            return response;
        }
        
        // Enforce max page size
        int pageNum = Math.max(0, page != null ? page : 0);
        int pageSize = Math.min(100, Math.max(1, size != null ? size : 20));
        
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Get only completed posts
        Page<EventFeedPost> postPage = postRepository.findByEventIdAndMediaUploadStatusOrderByCreatedAtDesc(
                eventId, MediaUploadStatus.COMPLETED, pageable);
        
        // Enrich with engagement data
        List<FeedPostResponse> enrichedPosts = enrichPostsWithEngagement(postPage.getContent(), eventId, principal);
        
        PostListResponse response = new PostListResponse();
        response.setPosts(enrichedPosts);
        response.setCurrentPage(pageNum);
        response.setPageSize(pageSize);
        response.setTotalPosts(postPage.getTotalElements());
        response.setTotalPages(postPage.getTotalPages());
        response.setHasNext(postPage.hasNext());
        response.setHasPrevious(postPage.hasPrevious());
        
        return response;
    }

    public FeedPostResponse get(UUID eventId, UUID postId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        EventFeedPost post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (post.getEvent() == null || !eventId.equals(post.getEvent().getId())) {
            throw new IllegalArgumentException("Post not found");
        }
        // Only return if completed (null status = existing post, treat as completed)
        // Or allow creator to see their own pending posts
        MediaUploadStatus status = post.getMediaUploadStatus();
        if (status != null && status != MediaUploadStatus.COMPLETED) {
            if (principal == null || post.getCreatedBy() == null || !post.getCreatedBy().getId().equals(principal.getId())) {
                throw new IllegalArgumentException("Post not found");
            }
        }
        return toResponse(post, eventId);
    }

    public void delete(UUID eventId, UUID postId, UserPrincipal principal) {
        accessControlService.requireMediaUpload(principal, eventId);
        EventFeedPost post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (post.getEvent() == null || !eventId.equals(post.getEvent().getId())) {
            throw new IllegalArgumentException("Post not found");
        }

        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        boolean isCreator = post.getCreatedBy() != null && post.getCreatedBy().getId().equals(principal.getId());
        if (!isCreator) {
            accessControlService.requireMediaManage(principal, eventId);
        }

        postRepository.delete(post);
    }

    private List<FeedPostResponse> enrichPostsWithEngagement(List<EventFeedPost> posts, UUID eventId, UserPrincipal principal) {
        if (posts.isEmpty()) {
            return List.of();
        }

        // Extract author information from relationships (already loaded or will be lazy loaded)
        Map<UUID, UserAccount> authorsById = posts.stream()
                .map(EventFeedPost::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity(), (a, b) -> a));

        // Load engagement counts using batch queries (3 queries instead of 3*N)
        List<UUID> postIds = posts.stream().map(EventFeedPost::getId).collect(Collectors.toList());
        Map<UUID, Long> likeCounts = likeService.getLikeCountBatch(postIds);
        Map<UUID, Long> commentCounts = commentService.getCommentCountBatch(postIds);
        
        UUID currentUserId = principal != null ? principal.getId() : null;
        Map<UUID, Boolean> likedByUser = currentUserId != null 
                ? likeService.isLikedBatch(postIds, currentUserId)
                : Map.of();

        return posts.stream()
                .map(post -> toResponse(post, eventId, authorsById, likeCounts, commentCounts, likedByUser))
                .collect(Collectors.toList());
    }

    private FeedPostResponse toResponse(EventFeedPost post, UUID eventId) {
        return toResponse(post, eventId, Map.of(), Map.of(), Map.of(), Map.of());
    }

    private FeedPostResponse toResponse(EventFeedPost post, UUID eventId,
                                        Map<UUID, UserAccount> authorsById,
                                        Map<UUID, Long> likeCounts,
                                        Map<UUID, Long> commentCounts,
                                        Map<UUID, Boolean> likedByUser) {
        FeedPostResponse resp = new FeedPostResponse();
        resp.setId(post.getId());
        resp.setEventId(post.getEvent() != null ? post.getEvent().getId() : eventId);
        resp.setType(post.getPostType() != null ? post.getPostType().name() : "TEXT");
        resp.setContent(post.getContent());
        resp.setMediaObjectId(post.getMediaObjectId());
        resp.setCreatedAt(post.getCreatedAt());
        resp.setUpdatedAt(post.getUpdatedAt());

        // Set author information from relationship
        if (post.getCreatedBy() != null) {
            resp.setCreatedBy(post.getCreatedBy().getId());
            resp.setAuthorName(post.getCreatedBy().getName());
            resp.setAuthorAvatarUrl(post.getCreatedBy().getProfilePictureUrl());
        } else {
            resp.setCreatedBy(null);
        }

        // Set engagement counts
        resp.setLikeCount(likeCounts.getOrDefault(post.getId(), 0L));
        resp.setCommentCount(commentCounts.getOrDefault(post.getId(), 0L));
        resp.setIsLiked(likedByUser.getOrDefault(post.getId(), false));
        resp.setRepostCount(post.getRepostCount() != null ? post.getRepostCount() : 0L);

        // Set repost information
        if (post.getRepostedFrom() != null) {
            EventFeedPost originalPost = post.getRepostedFrom();
            resp.setRepostedFromId(originalPost.getId());
            resp.setQuoteText(post.getQuoteText());

            // Build original post info
            FeedPostResponse.OriginalPost originalPostInfo = new FeedPostResponse.OriginalPost();
            originalPostInfo.setId(originalPost.getId());
            originalPostInfo.setType(originalPost.getPostType() != null ? originalPost.getPostType().name() : "TEXT");
            originalPostInfo.setContent(originalPost.getContent());
            originalPostInfo.setCreatedAt(originalPost.getCreatedAt());

            if (originalPost.getCreatedBy() != null) {
                originalPostInfo.setAuthorId(originalPost.getCreatedBy().getId());
                originalPostInfo.setAuthorName(originalPost.getCreatedBy().getName());
                originalPostInfo.setAuthorAvatarUrl(originalPost.getCreatedBy().getProfilePictureUrl());
            }

            // Add media URL for original post if it has media
            if (originalPost.getMediaObjectId() != null) {
                EventStoredObject obj = storedObjectRepository.findById(originalPost.getMediaObjectId()).orElse(null);
                if (obj != null && obj.getEvent() != null && PURPOSE_POST_MEDIA.equals(obj.getPurpose())) {
                    URL presignedGet = storageService.generatePresignedGetUrl(EVENT_BUCKET_ALIAS, obj.getObjectKey(), DOWNLOAD_URL_TTL);
                    originalPostInfo.setMediaUrl(presignedGet.toString());
                }
            }

            resp.setOriginalPost(originalPostInfo);
        }

        if (post.getMediaObjectId() != null) {
            EventStoredObject obj = storedObjectRepository.findById(post.getMediaObjectId()).orElse(null);
            if (obj != null && obj.getEvent() != null && eventId.equals(obj.getEvent().getId()) && PURPOSE_POST_MEDIA.equals(obj.getPurpose())) {
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

    /**
     * Scheduled task that runs to clean up incomplete uploads.
     * Cron expression format: second, minute, hour, day, month, weekday
     */
    @Scheduled(cron = "${feeds.cleanup.cron}")
    @Transactional
    public void cleanupIncompleteUploads() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minus(maxAgeMinutes, ChronoUnit.MINUTES);
            List<EventFeedPost> incomplete = postRepository.findByMediaUploadStatusAndCreatedAtBefore(
                    MediaUploadStatus.PENDING,
                    cutoff
            );

            if (incomplete.isEmpty()) {
                log.debug("No incomplete feed posts to clean up");
                return;
            }

            log.info("Cleaning up {} incomplete feed posts older than {} minutes", incomplete.size(), maxAgeMinutes);

            int deletedCount = 0;
            int s3CleanupCount = 0;
            int s3CleanupFailures = 0;

            for (EventFeedPost post : incomplete) {
                try {
                    // Delete orphaned S3 object if it exists
                    if (post.getMediaObjectId() != null && post.getEvent() != null) {
                        try {
                            String objectKey = buildObjectKey(post.getEvent().getId(), post.getMediaObjectId());
                            storageService.deleteObject(EVENT_BUCKET_ALIAS, objectKey);
                            s3CleanupCount++;
                            log.debug("Deleted S3 object {} for incomplete post {}", objectKey, post.getId());
                        } catch (Exception e) {
                            s3CleanupFailures++;
                            log.warn("Failed to delete S3 object for post {}: {}", post.getId(), e.getMessage());
                            // Continue with post deletion even if S3 cleanup fails
                        }
                    }
                    
                    // Delete the incomplete post
                    postRepository.delete(post);
                    deletedCount++;
                    
                } catch (Exception e) {
                    log.error("Failed to cleanup incomplete post {}: {}", post.getId(), e.getMessage(), e);
                }
            }

            log.info("Cleanup complete: {} posts deleted, {} S3 objects cleaned, {} S3 cleanup failures", 
                    deletedCount, s3CleanupCount, s3CleanupFailures);
                    
        } catch (Exception e) {
            log.error("Error in scheduled feed post cleanup", e);
        }
    }

    private static int requireConfigured(Integer value, String propertyName) {
        if (value == null) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
        return value;
    }

    /**
     * Repost a post - creates a new post that references the original
     * @param eventId The event ID
     * @param postId The original post ID
     * @param principal The user principal
     * @param quoteText Optional quote text for quote posts
     * @return The reposted post response
     */
    public FeedPostResponse repost(UUID eventId, UUID postId, UserPrincipal principal, String quoteText) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        // Get the original post
        EventFeedPost originalPost = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        // Verify the post belongs to the event
        if (!originalPost.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Post does not belong to this event");
        }

        // Verify access to the event (requireMediaView checks access and throws if denied)
        accessControlService.requireMediaView(principal, eventId);

        // Check if user already reposted this post
        boolean alreadyReposted = postRepository.existsByCreatedByIdAndRepostedFromId(
                principal.getId(), originalPost.getId());
        if (alreadyReposted) {
            throw new IllegalArgumentException("You have already reposted this post");
        }

        // Create a new post that references the original
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        UserAccount author = userAccountRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        EventFeedPost repost = new EventFeedPost();
        repost.setEvent(event);
        repost.setCreatedBy(author);
        repost.setPostType(EventFeedPost.PostType.TEXT);
        repost.setRepostedFrom(originalPost);
        repost.setMediaUploadStatus(MediaUploadStatus.COMPLETED);

        // Handle quote text
        if (quoteText != null && !quoteText.trim().isEmpty()) {
            String trimmedQuote = quoteText.trim();
            if (trimmedQuote.length() > 4000) {
                throw new IllegalArgumentException("Quote text too long (max 4000 characters)");
            }
            repost.setQuoteText(trimmedQuote);
        }

        // Save repost first, then atomically increment count on original post
        EventFeedPost saved = postRepository.save(repost);

        // Atomic increment prevents race conditions with concurrent reposts
        postRepository.incrementRepostCount(originalPost.getId());

        // Convert to response
        return enrichPostsWithEngagement(List.of(saved), eventId, principal).get(0);
    }

    /**
     * Simple repost without quote text
     */
    public FeedPostResponse repost(UUID eventId, UUID postId, UserPrincipal principal) {
        return repost(eventId, postId, principal, null);
    }

    /**
     * Get all posts created by a user
     */
    public PostListResponse getUserPosts(UUID userId, Integer page, Integer size, UserPrincipal principal) {
        // Enforce max page size
        int pageNum = Math.max(0, page != null ? page : 0);
        int pageSize = Math.min(100, Math.max(1, size != null ? size : 20));

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Get only completed posts by user
        Page<EventFeedPost> postPage = postRepository.findByCreatedByUserIdAndMediaUploadStatusOrderByCreatedAtDesc(
                userId, MediaUploadStatus.COMPLETED, pageable);

        // Enrich with engagement data, filtering out posts from events the viewer cannot access
        List<FeedPostResponse> enrichedPosts = new ArrayList<>();
        for (EventFeedPost post : postPage.getContent()) {
            if (post.getEvent() != null && post.getEvent().getId() != null) {
                // Check if viewer has access to this event's content
                try {
                    accessControlService.requireMediaView(principal, post.getEvent().getId());
                    enrichedPosts.add(toResponse(post, post.getEvent().getId()));
                } catch (ForbiddenException e) {
                    // Skip posts from private events the viewer cannot access
                    log.debug("User {} cannot view post {} from event {}: {}",
                            principal != null ? principal.getId() : "anonymous",
                            post.getId(), post.getEvent().getId(), e.getMessage());
                } catch (UnauthorizedException e) {
                    // Skip posts that require authentication
                    log.debug("Unauthenticated user cannot view post {} from event {}: {}",
                            post.getId(), post.getEvent().getId(), e.getMessage());
                }
            }
        }

        PostListResponse response = new PostListResponse();
        response.setPosts(enrichedPosts);
        response.setCurrentPage(pageNum);
        response.setPageSize(pageSize);
        // Note: totalPosts reflects all user posts, not just accessible ones
        // This is intentional to avoid leaking information about private event count
        response.setTotalPosts((long) enrichedPosts.size());
        response.setTotalPages((int) Math.ceil((double) enrichedPosts.size() / pageSize));
        response.setHasNext(false); // Conservative - cannot determine without querying
        response.setHasPrevious(pageNum > 0);

        return response;
    }
}
