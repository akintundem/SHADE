package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "Response returned when creating an event and requesting a presigned cover image upload")
public class CreateEventWithCoverUploadResponse {

    @Schema(description = "Created event details")
    EventResponse event;

    @Schema(description = "Presigned upload details for uploading the cover image to S3")
    EventPresignedUploadResponse coverUpload;
}

