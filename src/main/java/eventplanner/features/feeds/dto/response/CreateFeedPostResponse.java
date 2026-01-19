package eventplanner.features.feeds.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "Response returned when creating a feed post, optionally including a presigned media upload")
public class CreateFeedPostResponse {
    FeedPostResponse post;
    PresignedUploadResponse mediaUpload; // null for TEXT posts
}


