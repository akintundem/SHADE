package eventplanner.features.event.controller;

import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.dto.ApiMessageResponse;
import eventplanner.features.event.dto.request.EventMediaRequest;
import eventplanner.features.event.dto.request.EventMediaUploadRequest;
import eventplanner.features.event.dto.response.EventCoverImageResponse;
import eventplanner.features.event.dto.response.EventMediaResponse;
import eventplanner.features.event.dto.response.EventPresignedUploadResponse;
import eventplanner.features.event.service.EventMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Media", description = "Event media and assets management operations")
@SecurityRequirement(name = "bearerAuth")
public class EventMediaController {

    private final EventMediaService mediaService;

    public EventMediaController(EventMediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping("/{id}/media")
    @Operation(summary = "Get event media", description = "Get all media associated with an event")
    public ResponseEntity<List<EventMediaResponse>> getEventMedia(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Media category filter") @RequestParam(required = false) String category,
            @Parameter(description = "Media type filter") @RequestParam(required = false) String type) {
        List<EventMediaResponse> media = mediaService.getEventMedia(id, principal, category, type);
        return ResponseEntity.ok(media);
    }

    @PostMapping("/{id}/media")
    @Operation(summary = "Upload event media", description = "Upload media for an event")
    public ResponseEntity<EventPresignedUploadResponse> uploadMedia(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EventMediaUploadRequest request) {
        EventPresignedUploadResponse response = mediaService.createMediaUpload(id, principal, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/media/{mediaId}")
    @Operation(summary = "Get specific media", description = "Get details of specific media")
    public ResponseEntity<EventMediaResponse> getMedia(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Media ID") @PathVariable UUID mediaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        EventMediaResponse response = mediaService.getMedia(id, mediaId, principal);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/media/{mediaId}")
    @Operation(summary = "Update media", description = "Update media information")
    public ResponseEntity<EventMediaResponse> updateMedia(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Media ID") @PathVariable UUID mediaId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EventMediaRequest request) {
        EventMediaResponse response = mediaService.updateMedia(id, mediaId, principal, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    @Operation(summary = "Delete media", description = "Delete media from event")
    public ResponseEntity<ApiMessageResponse> deleteMedia(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Media ID") @PathVariable UUID mediaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ApiMessageResponse response = mediaService.deleteMedia(id, mediaId, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/assets")
    @Operation(summary = "Get event assets", description = "Get all assets associated with an event")
    public ResponseEntity<List<EventMediaResponse>> getEventAssets(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<EventMediaResponse> assets = mediaService.getEventAssets(id, principal);
        return ResponseEntity.ok(assets);
    }

    @PostMapping("/{id}/assets")
    @Operation(summary = "Upload event asset", description = "Upload an asset for an event")
    public ResponseEntity<EventPresignedUploadResponse> uploadAsset(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EventMediaUploadRequest request) {
        EventPresignedUploadResponse response = mediaService.createAssetUpload(id, principal, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cover-image")
    @Operation(summary = "Update cover image", description = "Update the cover image for an event")
    public ResponseEntity<EventPresignedUploadResponse> updateCoverImage(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EventMediaUploadRequest request) {
        EventPresignedUploadResponse response = mediaService.createCoverImageUpload(id, principal, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/cover-image")
    @Operation(summary = "Remove cover image", description = "Remove the cover image from an event")
    public ResponseEntity<EventCoverImageResponse> removeCoverImage(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        EventCoverImageResponse response = mediaService.removeCoverImage(id, principal);
        return ResponseEntity.ok(response);
    }
}
