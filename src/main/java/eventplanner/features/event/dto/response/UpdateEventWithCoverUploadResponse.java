package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "Response returned when updating an event and optionally requesting a presigned cover image upload")
public class UpdateEventWithCoverUploadResponse {

    @Schema(description = "Updated event details")
    EventResponse event;

    @Schema(description = "Presigned upload details for uploading the cover image to S3 (null when no cover update requested)")
    EventPresignedUploadResponse coverUpload;
}

