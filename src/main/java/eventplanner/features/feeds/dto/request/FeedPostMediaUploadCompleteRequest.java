package eventplanner.features.feeds.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Complete a post media upload after uploading to S3 via presigned URL")
public class FeedPostMediaUploadCompleteRequest {

    @NotBlank
    @Size(max = 512)
    private String objectKey;

    @Size(max = 2048)
    private String resourceUrl;

    @NotBlank
    @Size(max = 255)
    private String fileName;

    @NotBlank
    @Size(max = 255)
    private String contentType;

    @Schema(description = "Whether object is public (usually false)")
    private Boolean isPublic = false;

    @Schema(description = "Optional description")
    private String description;

    @Schema(description = "Optional tags")
    private String tags;

    @Schema(description = "Optional metadata JSON string")
    private String metadata;
}


