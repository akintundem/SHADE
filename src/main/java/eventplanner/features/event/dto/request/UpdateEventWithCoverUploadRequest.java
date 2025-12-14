package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for updating an event and optionally requesting a presigned cover image upload.
 *
 * Flow:
 * 1) Client calls PUT /events/{id} with event updates and optional coverUpload metadata.
 * 2) Backend updates event fields (excluding coverImageUrl).
 * 3) If coverUpload is provided, backend returns presigned URL details.
 * 4) Client uploads directly to S3 using the presigned URL.
 * 5) Client calls the existing cover-image complete endpoint to persist coverImageUrl on the event.
 */
@Schema(description = "Update event request with optional cover image presigned upload details")
@Getter
@Setter
public class UpdateEventWithCoverUploadRequest {

    @Valid
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Event update payload")
    private UpdateEventRequest event;

    @Valid
    @Schema(description = "Optional cover image upload metadata (used to generate presigned URL). If omitted, cover image is unchanged.")
    private EventMediaUploadRequest coverUpload;
}

