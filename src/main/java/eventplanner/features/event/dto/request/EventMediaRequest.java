package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for event media upload
 */
@Schema(description = "Event media upload request")
@Getter
@Setter
public class EventMediaRequest {

    @NotBlank(message = "Media type is required")
    @Schema(description = "Media type (image, video, document)", example = "image")
    private String mediaType;

    @NotBlank(message = "Media name is required")
    @Schema(description = "Media name/title")
    private String mediaName;

    @Schema(description = "Media description")
    private String description;

    @Schema(description = "Media category (cover, gallery, document, etc.)")
    private String category;

    @Schema(description = "Whether media is public")
    private Boolean isPublic = true;

    @Schema(description = "Media tags")
    private String tags;

    @Schema(description = "Media file (for upload)")
    private MultipartFile file;

    @Schema(description = "Media URL (for external media)")
    private String mediaUrl;

    @Schema(description = "Media metadata")
    private String metadata;
}
