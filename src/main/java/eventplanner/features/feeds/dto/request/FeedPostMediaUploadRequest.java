package eventplanner.features.feeds.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request a presigned upload for a post media file")
public class FeedPostMediaUploadRequest {

    @NotBlank
    @Size(max = 255)
    private String fileName;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "MIME type", example = "image/jpeg")
    private String contentType;

    @Schema(description = "Whether the underlying object should be marked public (usually false)")
    private Boolean isPublic = false;

    @Schema(description = "Optional description")
    private String description;
}


