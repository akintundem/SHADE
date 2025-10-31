package ai.eventplanner.event.service;

import ai.eventplanner.auth.service.UserPrincipal;
import ai.eventplanner.common.dto.ApiMessageResponse;
import ai.eventplanner.event.dto.request.EventMediaRequest;
import ai.eventplanner.event.dto.request.EventMediaUploadRequest;
import ai.eventplanner.event.dto.response.EventCoverImageResponse;
import ai.eventplanner.event.dto.response.EventMediaResponse;
import ai.eventplanner.event.dto.response.EventPresignedUploadResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventMediaService {

    private final EventAccessControlService accessControlService;

    public EventMediaService(EventAccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public List<EventMediaResponse> getEventMedia(UUID eventId, UserPrincipal principal, String category, String type) {
        accessControlService.requireMediaView(principal, eventId);
        return new ArrayList<>();
    }

    public EventPresignedUploadResponse createMediaUpload(UUID eventId, UserPrincipal principal, EventMediaUploadRequest request) {
        accessControlService.requireMediaUpload(principal, eventId);
        return buildPresignedResponse(eventId, request, false);
    }

    public EventMediaResponse getMedia(UUID eventId, UUID mediaId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        EventMediaResponse response = new EventMediaResponse();
        response.setMediaId(mediaId);
        response.setEventId(eventId);
        response.setMediaType("image");
        response.setMediaName("Sample Media");
        response.setDescription("Sample media description");
        response.setCategory("gallery");
        response.setMediaUrl("https://storage.example.com/media/" + mediaId);
        response.setThumbnailUrl("https://storage.example.com/thumbnails/" + mediaId);
        response.setFileSize(1024L);
        response.setMimeType("image/jpeg");
        response.setIsPublic(Boolean.TRUE);
        response.setUploadedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    public EventMediaResponse updateMedia(UUID eventId, UUID mediaId, UserPrincipal principal, EventMediaRequest request) {
        accessControlService.requireMediaManage(principal, eventId);
        EventMediaResponse response = new EventMediaResponse();
        response.setMediaId(mediaId);
        response.setEventId(eventId);
        response.setMediaType(request.getMediaType());
        response.setMediaName(request.getMediaName());
        response.setDescription(request.getDescription());
        response.setCategory(request.getCategory());
        response.setIsPublic(request.getIsPublic());
        response.setTags(request.getTags());
        response.setMetadata(request.getMetadata());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    public ApiMessageResponse deleteMedia(UUID eventId, UUID mediaId, UserPrincipal principal) {
        accessControlService.requireMediaManage(principal, eventId);
        return ApiMessageResponse.success("Media deleted successfully");
    }

    public List<EventMediaResponse> getEventAssets(UUID eventId, UserPrincipal principal) {
        accessControlService.requireAssetView(principal, eventId);
        return new ArrayList<>();
    }

    public EventPresignedUploadResponse createAssetUpload(UUID eventId, UserPrincipal principal, EventMediaUploadRequest request) {
        accessControlService.requireAssetView(principal, eventId);
        return buildPresignedResponse(eventId, request, true);
    }

    public EventPresignedUploadResponse createCoverImageUpload(UUID eventId, UserPrincipal principal, EventMediaUploadRequest request) {
        accessControlService.requireCoverManage(principal, eventId);
        return buildPresignedResponse(eventId, request, false);
    }

    public EventCoverImageResponse removeCoverImage(UUID eventId, UserPrincipal principal) {
        accessControlService.requireCoverManage(principal, eventId);
        return EventCoverImageResponse.builder()
            .eventId(eventId)
            .coverImageUrl(null)
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private EventPresignedUploadResponse buildPresignedResponse(UUID eventId, EventMediaUploadRequest request, boolean asset) {
        UUID mediaId = UUID.randomUUID();
        String objectPath = asset ? "assets" : "media";
        String uploadUrl = "https://storage.example.com/upload/" + objectPath + "/" + mediaId;
        String resourceUrl = "https://storage.example.com/" + objectPath + "/" + mediaId;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        Map<String, String> headers = Map.of("Content-Type", request.getContentType());

        return EventPresignedUploadResponse.builder()
            .mediaId(mediaId)
            .uploadMethod("PUT")
            .uploadUrl(uploadUrl)
            .headers(headers)
            .resourceUrl(resourceUrl)
            .expiresAt(expiresAt)
            .build();
    }
}
