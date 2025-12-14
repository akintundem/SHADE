package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for creating an event and requesting a presigned cover image upload.
 *
 * Flow:
 * 1) Client calls this endpoint with event details + cover upload metadata (fileName, contentType)
 * 2) Backend creates the event
 * 3) Backend returns EventResponse + presigned upload URL for cover image
 * 4) Client uploads directly to S3 using the presigned URL
 * 5) Client calls the existing complete-cover-upload endpoint to persist coverImageUrl on the event
 */
@Schema(description = "Create event request with cover image presigned upload details")
@Getter
@Setter
public class CreateEventWithCoverUploadRequest {

    @Valid
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Event creation payload")
    private CreateEventRequest event;

    @Valid
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Cover image upload metadata (used to generate presigned URL)")
    private EventMediaUploadRequest coverUpload;
}

