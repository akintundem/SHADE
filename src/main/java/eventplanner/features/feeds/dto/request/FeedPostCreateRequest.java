package eventplanner.features.feeds.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Create an event feed post (text/image/video). For IMAGE/VIDEO, provide mediaUpload so the API can return a presigned S3 upload.")
public class FeedPostCreateRequest {

    @NotNull
    @Schema(description = "Post type", example = "TEXT")
    private String type;

    @Schema(description = "Text content (for TEXT posts or caption)", example = "Hello world")
    private String content;

    @Valid
    @Schema(description = "Media upload request (required for IMAGE/VIDEO)")
    private FeedPostMediaUploadRequest mediaUpload;
}


