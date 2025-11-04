package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for event media
 */
@Schema(description = "Event media response")
@Getter
@Setter
public class EventMediaResponse {

    @Schema(description = "Media ID")
    private UUID mediaId;

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Media type")
    private String mediaType;

    @Schema(description = "Media name")
    private String mediaName;

    @Schema(description = "Media description")
    private String description;

    @Schema(description = "Media category")
    private String category;

    @Schema(description = "Media URL")
    private String mediaUrl;

    @Schema(description = "Thumbnail URL")
    private String thumbnailUrl;

    @Schema(description = "File size in bytes")
    private Long fileSize;

    @Schema(description = "File MIME type")
    private String mimeType;

    @Schema(description = "Media width (for images/videos)")
    private Integer width;

    @Schema(description = "Media height (for images/videos)")
    private Integer height;

    @Schema(description = "Media duration (for videos/audio)")
    private Long duration;

    @Schema(description = "Whether media is public")
    private Boolean isPublic;

    @Schema(description = "Media tags")
    private String tags;

    @Schema(description = "Media metadata")
    private String metadata;

    @Schema(description = "Upload timestamp")
    private LocalDateTime uploadedAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
