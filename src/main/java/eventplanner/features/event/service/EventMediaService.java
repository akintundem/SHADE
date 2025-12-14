package eventplanner.features.event.service;

import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.dto.ApiMessageResponse;
import eventplanner.common.exception.BadRequestException;
import eventplanner.common.storage.s3.services.S3StorageService;
import eventplanner.features.event.dto.request.EventMediaRequest;
import eventplanner.features.event.dto.request.EventMediaUploadRequest;
import eventplanner.features.event.dto.request.EventMediaUploadCompleteRequest;
import eventplanner.features.event.dto.response.EventCoverImageResponse;
import eventplanner.features.event.dto.response.EventMediaResponse;
import eventplanner.features.event.dto.response.EventPresignedUploadResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventStoredObject;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.event.repository.EventStoredObjectRepository;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventMediaService {

    private static final String EVENT_BUCKET_ALIAS = "event";
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(10);

    private static final String PURPOSE_EVENT_MEDIA = "event_media";
    private static final String PURPOSE_EVENT_ASSET = "event_asset";
    private static final String PURPOSE_EVENT_COVER = "event_cover";

    private final EventAccessControlService accessControlService;
    private final S3StorageService storageService;
    private final EventStoredObjectRepository storedObjectRepository;
    private final EventRepository eventRepository;

    public EventMediaService(EventAccessControlService accessControlService,
                             S3StorageService storageService,
                             EventStoredObjectRepository storedObjectRepository,
                             EventRepository eventRepository) {
        this.accessControlService = accessControlService;
        this.storageService = storageService;
        this.storedObjectRepository = storedObjectRepository;
        this.eventRepository = eventRepository;
    }

    public List<EventMediaResponse> getEventMedia(UUID eventId, UserPrincipal principal, String category, String type) {
        accessControlService.requireMediaView(principal, eventId);
        List<EventStoredObject> items = storedObjectRepository.findByEventIdAndPurposeOrderByCreatedAtDesc(eventId, PURPOSE_EVENT_MEDIA);
        List<EventMediaResponse> out = new ArrayList<>();
        for (EventStoredObject item : items) {
            if (StringUtils.hasText(category) && (item.getCategory() == null || !item.getCategory().equalsIgnoreCase(category))) {
                continue;
            }
            if (StringUtils.hasText(type) && (item.getContentType() == null || !item.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith(type.toLowerCase(java.util.Locale.ROOT)))) {
                continue;
            }
            out.add(toMediaResponse(eventId, item));
        }
        return out;
    }

    public EventPresignedUploadResponse createMediaUpload(UUID eventId, UserPrincipal principal, EventMediaUploadRequest request) {
        accessControlService.requireMediaUpload(principal, eventId);
        return buildPresignedResponse(eventId, request, PURPOSE_EVENT_MEDIA);
    }

    public EventMediaResponse completeMediaUpload(UUID eventId, UUID mediaId, UserPrincipal principal, EventMediaUploadCompleteRequest request) {
        accessControlService.requireMediaUpload(principal, eventId);
        EventStoredObject saved = completeStoredObject(eventId, mediaId, principal, request, PURPOSE_EVENT_MEDIA, eventId, null);
        return toMediaResponse(eventId, saved);
    }

    public EventMediaResponse getMedia(UUID eventId, UUID mediaId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        EventStoredObject item = requireStoredObject(eventId, mediaId, PURPOSE_EVENT_MEDIA);
        return toMediaResponse(eventId, item);
    }

    public EventMediaResponse updateMedia(UUID eventId, UUID mediaId, UserPrincipal principal, EventMediaRequest request) {
        accessControlService.requireMediaManage(principal, eventId);
        EventStoredObject item = requireStoredObject(eventId, mediaId, PURPOSE_EVENT_MEDIA);
        if (request.getMediaName() != null) {
            item.setFileName(request.getMediaName());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            item.setCategory(request.getCategory());
        }
        if (request.getIsPublic() != null) {
            item.setIsPublic(request.getIsPublic());
        }
        if (request.getTags() != null) {
            item.setTags(request.getTags());
        }
        if (request.getMetadata() != null) {
            item.setMetadata(request.getMetadata());
        }
        EventStoredObject saved = storedObjectRepository.save(item);
        return toMediaResponse(eventId, saved);
    }

    public ApiMessageResponse deleteMedia(UUID eventId, UUID mediaId, UserPrincipal principal) {
        accessControlService.requireMediaManage(principal, eventId);
        EventStoredObject item = requireStoredObject(eventId, mediaId, PURPOSE_EVENT_MEDIA);
        storageService.deleteObject(EVENT_BUCKET_ALIAS, item.getObjectKey());
        storedObjectRepository.delete(item);
        return ApiMessageResponse.success("Media deleted successfully");
    }

    public List<EventMediaResponse> getEventAssets(UUID eventId, UserPrincipal principal) {
        accessControlService.requireAssetView(principal, eventId);
        List<EventStoredObject> items = storedObjectRepository.findByEventIdAndPurposeOrderByCreatedAtDesc(eventId, PURPOSE_EVENT_ASSET);
        List<EventMediaResponse> out = new ArrayList<>();
        for (EventStoredObject item : items) {
            out.add(toMediaResponse(eventId, item));
        }
        return out;
    }

    public EventPresignedUploadResponse createAssetUpload(UUID eventId, UserPrincipal principal, EventMediaUploadRequest request) {
        accessControlService.requireAssetView(principal, eventId);
        return buildPresignedResponse(eventId, request, PURPOSE_EVENT_ASSET);
    }

    public EventMediaResponse completeAssetUpload(UUID eventId, UUID assetId, UserPrincipal principal, EventMediaUploadCompleteRequest request) {
        accessControlService.requireAssetView(principal, eventId);
        EventStoredObject saved = completeStoredObject(eventId, assetId, principal, request, PURPOSE_EVENT_ASSET, eventId, null);
        return toMediaResponse(eventId, saved);
    }

    public EventPresignedUploadResponse createCoverImageUpload(UUID eventId, UserPrincipal principal, EventMediaUploadRequest request) {
        accessControlService.requireCoverManage(principal, eventId);
        // Extra guard: cover image must be an image/*
        if (!StringUtils.hasText(request.getContentType()) || !request.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Cover image must be an image/* content type");
        }
        return buildPresignedResponse(eventId, request, PURPOSE_EVENT_COVER);
    }

    public EventCoverImageResponse completeCoverImageUpload(UUID eventId, UUID coverId, UserPrincipal principal, EventMediaUploadCompleteRequest request) {
        accessControlService.requireCoverManage(principal, eventId);
        EventStoredObject saved = completeStoredObject(eventId, coverId, principal, request, PURPOSE_EVENT_COVER, eventId, "cover");

        Event event = accessControlService.ensureEventExists(eventId);
        event.setCoverImageUrl(saved.getResourceUrl());
        eventRepository.save(event);

        return EventCoverImageResponse.builder()
            .eventId(eventId)
            .coverImageUrl(saved.getResourceUrl())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public EventCoverImageResponse removeCoverImage(UUID eventId, UserPrincipal principal) {
        accessControlService.requireCoverManage(principal, eventId);
        return EventCoverImageResponse.builder()
            .eventId(eventId)
            .coverImageUrl(null)
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private String buildObjectKey(UUID eventId, String purpose, UUID objectId) {
        // Deterministic key so the API can re-generate presigned GETs later without DB storage.
        return "events/" + eventId + "/" + pathSegment(purpose) + "/" + objectId;
    }

    private EventPresignedUploadResponse buildPresignedResponse(UUID eventId, EventMediaUploadRequest request, String purpose) {
        if (!storageService.isConfigured()) {
            throw new BadRequestException("S3_NOT_CONFIGURED", "S3 is not configured for uploads");
        }
        if (!StringUtils.hasText(request.getFileName())) {
            throw new IllegalArgumentException("fileName is required");
        }
        if (!StringUtils.hasText(request.getContentType())) {
            throw new IllegalArgumentException("contentType is required");
        }

        UUID objectId = UUID.randomUUID();
        String objectKey = buildObjectKey(eventId, purpose, objectId);
        URL presignedPut = storageService.generatePresignedPutUrl(EVENT_BUCKET_ALIAS, objectKey, UPLOAD_URL_TTL, request.getContentType());

        return EventPresignedUploadResponse.builder()
            .mediaId(objectId)
            .objectKey(objectKey)
            .uploadMethod("PUT")
            .uploadUrl(presignedPut.toString())
            .headers(Map.of("Content-Type", request.getContentType()))
            // Resource URL is the non-presigned object URL (may be inaccessible if bucket/object is private)
            .resourceUrl(storageService.stripQuery(presignedPut))
            .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(UPLOAD_URL_TTL))
            .build();
    }

    private EventMediaResponse toMediaResponse(UUID eventId, EventStoredObject item) {
        EventMediaResponse response = new EventMediaResponse();
        response.setMediaId(item.getId());
        response.setEventId(eventId);
        response.setMediaType(item.getPurpose());
        response.setMediaName(item.getFileName());
        response.setDescription(item.getDescription());
        response.setCategory(item.getCategory());
        response.setMimeType(item.getContentType());
        response.setIsPublic(item.getIsPublic());
        response.setTags(item.getTags());
        response.setMetadata(item.getMetadata());
        response.setUploadedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());

        URL presignedGet = storageService.generatePresignedGetUrl(EVENT_BUCKET_ALIAS, item.getObjectKey(), DOWNLOAD_URL_TTL);
        response.setMediaUrl(presignedGet.toString());
        return response;
    }

    private EventStoredObject requireStoredObject(UUID eventId, UUID objectId, String purpose) {
        EventStoredObject item = storedObjectRepository.findById(objectId)
            .orElseThrow(() -> new IllegalArgumentException("Stored object not found"));
        if (!eventId.equals(item.getEventId()) || !purpose.equals(item.getPurpose())) {
            throw new IllegalArgumentException("Stored object not found");
        }
        return item;
    }

    private EventStoredObject completeStoredObject(UUID eventId,
                                                   UUID objectId,
                                                   UserPrincipal principal,
                                                   EventMediaUploadCompleteRequest request,
                                                   String purpose,
                                                   UUID ownerId,
                                                   String categoryOverride) {
        // accessControlService already enforced auth, but keep defensive validations for request payload.
        String expectedObjectKey = buildObjectKey(eventId, purpose, objectId);
        if (StringUtils.hasText(request.getObjectKey()) && !expectedObjectKey.equals(request.getObjectKey())) {
            throw new IllegalArgumentException("objectKey does not match expected key for this upload");
        }
        if (!StringUtils.hasText(request.getContentType())) {
            throw new IllegalArgumentException("contentType is required");
        }
        if (!StringUtils.hasText(request.getFileName())) {
            throw new IllegalArgumentException("fileName is required");
        }
        if (PURPOSE_EVENT_COVER.equals(purpose)) {
            if (!request.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
                throw new IllegalArgumentException("contentType must be image/*");
            }
        }

        EventStoredObject item = storedObjectRepository.findById(objectId).orElseGet(EventStoredObject::new);
        item.setId(objectId);
        item.setEventId(eventId);
        item.setPurpose(purpose);
        item.setOwnerId(ownerId);
        item.setObjectKey(expectedObjectKey);
        item.setResourceUrl(StringUtils.hasText(request.getResourceUrl())
            ? normalizeAndStripUrl(request.getResourceUrl())
            : null);
        item.setFileName(request.getFileName());
        item.setContentType(request.getContentType());
        item.setCategory(StringUtils.hasText(categoryOverride) ? categoryOverride : request.getCategory());
        item.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        item.setDescription(request.getDescription());
        item.setTags(request.getTags());
        item.setMetadata(request.getMetadata());
        item.setUploadedBy(principal != null ? principal.getId() : null);
        return storedObjectRepository.save(item);
    }

    private String normalizeAndStripUrl(String url) {
        try {
            return storageService.stripQuery(new URL(url.trim()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid resourceUrl");
        }
    }

    private String pathSegment(String purpose) {
        return switch (purpose) {
            case PURPOSE_EVENT_MEDIA -> "media";
            case PURPOSE_EVENT_ASSET -> "assets";
            case PURPOSE_EVENT_COVER -> "cover";
            default -> "objects";
        };
    }
}
