package ai.eventplanner.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Schema(description = "Request payload for generating a presigned upload URL for event media or assets")
@Getter
@Setter
public class EventMediaUploadRequest {

    @NotBlank
    @Schema(description = "Original file name", example = "presentation.pdf")
    private String fileName;

    @NotBlank
    @Schema(description = "MIME content type", example = "application/pdf")
    private String contentType;

    @Schema(description = "Optional category to group the media", example = "gallery")
    private String category;

    @Schema(description = "Whether the media should be publicly accessible after upload", defaultValue = "true")
    private Boolean isPublic = Boolean.TRUE;

    @Schema(description = "Comma separated tags")
    private String tags;

    @Schema(description = "Human readable description")
    private String description;

    @Schema(description = "Arbitrary metadata to attach to the upload", example = "{\"language\":\"en\"}")
    private Map<String, String> metadata;
}
