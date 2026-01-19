package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Sent by the client AFTER it uploads directly to S3 using a presigned URL.
 * This lets the backend persist the uploaded object's key/url + metadata.
 */
@Getter
@Setter
@Schema(description = "Request payload for completing an event media/asset upload after S3 upload finishes")
public class EventMediaUploadCompleteRequest {

    @Schema(description = "S3 object key (should match the objectKey from the presign response)")
    private String objectKey;

    @Schema(description = "Non-presigned resource URL (no query params). Optional; backend will normalize if provided.")
    private String resourceUrl;

    @NotBlank
    @Schema(description = "Original file name", example = "photo.jpg")
    private String fileName;

    @NotBlank
    @Schema(description = "MIME content type", example = "image/jpeg")
    private String contentType;

    @Schema(description = "Optional category to group the media", example = "gallery")
    private String category;

    @Schema(description = "Whether the media should be publicly accessible after upload", defaultValue = "false")
    private Boolean isPublic = Boolean.FALSE;

    @Schema(description = "Comma separated tags")
    private String tags;

    @Schema(description = "Human readable description")
    private String description;

    @Schema(description = "Arbitrary metadata (stored as string/JSON)")
    private String metadata;
}


