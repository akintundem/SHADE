package ai.eventplanner.event.controller;

import ai.eventplanner.event.dto.request.EventMediaRequest;
import ai.eventplanner.event.dto.response.EventMediaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Event Media and Assets Controller
 * Handles event media upload, management, and assets
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Media", description = "Event media and assets management operations")
@SecurityRequirement(name = "bearerAuth")
public class EventMediaController {

    public EventMediaController() {
    }

    // ==================== EVENT MEDIA ENDPOINTS ====================

    @GetMapping("/{id}/media")
    @Operation(summary = "Get event media", description = "Get all media associated with an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Media retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<EventMediaResponse>> getEventMedia(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Media category filter") @RequestParam(required = false) String category,
            @Parameter(description = "Media type filter") @RequestParam(required = false) String type) {
        try {
            // For now, return empty list - this would be populated from a media table
            List<EventMediaResponse> media = new ArrayList<>();
            return ResponseEntity.ok(media);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/media")
    @Operation(summary = "Upload event media", description = "Upload media for an event")
    public ResponseEntity<EventMediaResponse> uploadMedia(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mediaType", defaultValue = "image") String mediaType,
            @RequestParam(value = "mediaName", required = false) String mediaName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "isPublic", defaultValue = "true") Boolean isPublic,
            @RequestParam(value = "tags", required = false) String tags) {
        try {
            EventMediaResponse response = new EventMediaResponse();
            response.setMediaId(UUID.randomUUID());
            response.setEventId(id);
            response.setMediaType(mediaType);
            response.setMediaName(mediaName != null ? mediaName : file.getOriginalFilename());
            response.setDescription(description);
            response.setCategory(category);
            response.setMediaUrl("https://storage.example.com/media/" + response.getMediaId());
            response.setThumbnailUrl("https://storage.example.com/thumbnails/" + response.getMediaId());
            response.setFileSize(file.getSize());
            response.setMimeType(file.getContentType());
            response.setIsPublic(isPublic);
            response.setTags(tags);
            response.setUploadedAt(LocalDateTime.now());
            response.setUpdatedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}/media/{mediaId}")
    @Operation(summary = "Get specific media", description = "Get details of specific media")
    public ResponseEntity<EventMediaResponse> getMedia(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Media ID") @PathVariable UUID mediaId) {
        try {
            EventMediaResponse response = new EventMediaResponse();
            response.setMediaId(mediaId);
            response.setEventId(id);
            response.setMediaType("image");
            response.setMediaName("Sample Media");
            response.setDescription("Sample media description");
            response.setCategory("gallery");
            response.setMediaUrl("https://storage.example.com/media/" + mediaId);
            response.setThumbnailUrl("https://storage.example.com/thumbnails/" + mediaId);
            response.setFileSize(1024000L);
            response.setMimeType("image/jpeg");
            response.setWidth(1920);
            response.setHeight(1080);
            response.setIsPublic(true);
            response.setUploadedAt(LocalDateTime.now());
            response.setUpdatedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/{id}/media/{mediaId}")
    @Operation(summary = "Update media", description = "Update media information")
    public ResponseEntity<EventMediaResponse> updateMedia(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Media ID") @PathVariable UUID mediaId,
            @Valid @RequestBody EventMediaRequest request) {
        try {
            EventMediaResponse response = new EventMediaResponse();
            response.setMediaId(mediaId);
            response.setEventId(id);
            response.setMediaType(request.getMediaType());
            response.setMediaName(request.getMediaName());
            response.setDescription(request.getDescription());
            response.setCategory(request.getCategory());
            response.setIsPublic(request.getIsPublic());
            response.setTags(request.getTags());
            response.setMetadata(request.getMetadata());
            response.setUpdatedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    @Operation(summary = "Delete media", description = "Delete media from event")
    public ResponseEntity<Void> deleteMedia(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Media ID") @PathVariable UUID mediaId) {
        try {
            // In a real implementation, this would delete the media from storage and database
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== EVENT ASSETS ENDPOINTS ====================

    @GetMapping("/{id}/assets")
    @Operation(summary = "Get event assets", description = "Get all assets associated with an event")
    public ResponseEntity<List<EventMediaResponse>> getEventAssets(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            // For now, return empty list - this would be populated from an assets table
            List<EventMediaResponse> assets = new ArrayList<>();
            return ResponseEntity.ok(assets);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/assets")
    @Operation(summary = "Upload event asset", description = "Upload an asset for an event")
    public ResponseEntity<EventMediaResponse> uploadAsset(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "assetName", required = false) String assetName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category) {
        try {
            EventMediaResponse response = new EventMediaResponse();
            response.setMediaId(UUID.randomUUID());
            response.setEventId(id);
            response.setMediaType("document");
            response.setMediaName(assetName != null ? assetName : file.getOriginalFilename());
            response.setDescription(description);
            response.setCategory(category != null ? category : "asset");
            response.setMediaUrl("https://storage.example.com/assets/" + response.getMediaId());
            response.setFileSize(file.getSize());
            response.setMimeType(file.getContentType());
            response.setIsPublic(false);
            response.setUploadedAt(LocalDateTime.now());
            response.setUpdatedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== COVER IMAGE ENDPOINTS ====================

    @PutMapping("/{id}/cover-image")
    @Operation(summary = "Update cover image", description = "Update the cover image for an event")
    public ResponseEntity<Map<String, Object>> updateCoverImage(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        try {
            // Update the event's cover image URL
            String coverImageUrl = "https://storage.example.com/covers/" + id;
            
            Map<String, Object> response = Map.of(
                    "eventId", id,
                    "coverImageUrl", coverImageUrl,
                    "updatedAt", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/{id}/cover-image")
    @Operation(summary = "Remove cover image", description = "Remove the cover image from an event")
    public ResponseEntity<Map<String, Object>> removeCoverImage(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("eventId", id);
            response.put("coverImageUrl", null);
            response.put("updatedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
