package eventplanner.common.storage.s3.services;

import eventplanner.common.storage.s3.UploadCompletionCallback;
import eventplanner.common.storage.s3.dto.PresignedUploadCompleteRequest;
import eventplanner.common.storage.s3.dto.PresignedUploadRequest;
import eventplanner.common.storage.s3.dto.PresignedUploadResponse;
import eventplanner.common.util.UserAccountUtil;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventStoredObject;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.event.repository.EventStoredObjectRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Generic service for handling presigned S3 uploads with status tracking.
 * Reusable across the application for any entity that needs async media uploads.
 */
@Service
@Transactional
public class PresignedUploadService {
    
    private static final Duration DEFAULT_UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final String DEFAULT_BUCKET_ALIAS = "event";
    
    private final S3StorageService storageService;
    private final EventRepository eventRepository;
    private final EventStoredObjectRepository storedObjectRepository;
    private final UserAccountRepository userAccountRepository;
    
    public PresignedUploadService(S3StorageService storageService,
                                  EventRepository eventRepository,
                                  EventStoredObjectRepository storedObjectRepository,
                                  UserAccountRepository userAccountRepository) {
        this.storageService = storageService;
        this.eventRepository = eventRepository;
        this.storedObjectRepository = storedObjectRepository;
        this.userAccountRepository = userAccountRepository;
    }
    
    /**
     * Generate a presigned upload URL for media upload
     * 
     * @param request Upload request with file metadata
     * @param objectKeyBuilder Function to build the S3 object key (e.g., (mediaId) -> "events/{eventId}/media/{mediaId}")
     * @param bucketAlias S3 bucket alias (defaults to "event")
     * @param uploadTtl Time-to-live for the presigned URL (defaults to 10 minutes)
     * @return Presigned upload response with mediaId and upload URL
     */
    public PresignedUploadResponse generatePresignedUpload(
            PresignedUploadRequest request,
            Function<UUID, String> objectKeyBuilder,
            String bucketAlias,
            Duration uploadTtl) {
        
        if (!StringUtils.hasText(request.getFileName())) {
            throw new IllegalArgumentException("fileName is required");
        }
        if (!StringUtils.hasText(request.getContentType())) {
            throw new IllegalArgumentException("contentType is required");
        }
        
        UUID mediaId = UUID.randomUUID();
        String objectKey = objectKeyBuilder.apply(mediaId);
        String bucket = bucketAlias != null ? bucketAlias : DEFAULT_BUCKET_ALIAS;
        Duration ttl = uploadTtl != null ? uploadTtl : DEFAULT_UPLOAD_URL_TTL;
        
        URL presignedPut = storageService.generatePresignedPutUrl(bucket, objectKey, ttl, request.getContentType());
        
        return PresignedUploadResponse.builder()
                .mediaId(mediaId)
                .objectKey(objectKey)
                .uploadMethod("PUT")
                .uploadUrl(presignedPut.toString())
                .headers(Map.of("Content-Type", request.getContentType()))
                .resourceUrl(storageService.stripQuery(presignedPut))
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(ttl))
                .build();
    }
    
    /**
     * Complete a media upload and persist metadata
     * 
     * @param eventId Event ID (for event-scoped uploads)
     * @param mediaId Media ID from the presigned upload response
     * @param ownerId Owner entity ID (e.g., postId, eventId)
     * @param purpose Purpose/path segment (e.g., "post_media", "event_cover")
     * @param request Completion request with upload metadata
     * @param expectedObjectKey Expected object key (validated against actual)
     * @param principal User who completed the upload
     * @param callback Optional callback for entity-specific completion logic
     * @return Saved EventStoredObject
     */
    public EventStoredObject completeUpload(
            UUID eventId,
            UUID mediaId,
            UUID ownerId,
            String purpose,
            PresignedUploadCompleteRequest request,
            String expectedObjectKey,
            UserPrincipal principal,
            UploadCompletionCallback callback) {
        
        // Validate object key matches expected
        if (StringUtils.hasText(expectedObjectKey) && !expectedObjectKey.equals(request.getObjectKey())) {
            throw new IllegalArgumentException("objectKey does not match expected key for this upload");
        }
        if (!StringUtils.hasText(request.getContentType())) {
            throw new IllegalArgumentException("contentType is required");
        }
        if (!StringUtils.hasText(request.getFileName())) {
            throw new IllegalArgumentException("fileName is required");
        }
        
        // Fetch Event entity
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        
        // Get managed UserAccount entity for JPA relationship (optional)
        UserAccount uploadedByUser = (principal != null && principal.getId() != null)
            ? UserAccountUtil.getManagedUserAccountOrThrow(principal, userAccountRepository, "User not found")
            : null;
        
        // Save stored object metadata
        EventStoredObject item = storedObjectRepository.findById(mediaId).orElseGet(EventStoredObject::new);
        item.setId(mediaId);
        item.setEvent(event);
        item.setPurpose(purpose);
        item.setOwnerId(ownerId);
        item.setObjectKey(request.getObjectKey());
        item.setResourceUrl(StringUtils.hasText(request.getResourceUrl())
                ? normalizeAndStripUrl(request.getResourceUrl())
                : null);
        item.setFileName(request.getFileName());
        item.setContentType(request.getContentType());
        item.setCategory(request.getCategory());
        item.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        item.setDescription(request.getDescription());
        item.setTags(request.getTags());
        item.setMetadata(request.getMetadata());
        item.setUploadedBy(uploadedByUser); // Set user entity relationship
        
        EventStoredObject saved = storedObjectRepository.save(item);
        
        // Call entity-specific completion callback
        if (callback != null) {
            callback.onUploadCompleted(ownerId, mediaId, principal);
        }
        
        return saved;
    }
    
    private String normalizeAndStripUrl(String url) {
        try {
            return storageService.stripQuery(new URL(url.trim()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid resourceUrl");
        }
    }
}
