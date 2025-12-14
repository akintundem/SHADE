package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Convenience wrapper to complete an event cover image upload without putting the coverId in the URL path.
 * Mirrors the "create presigned" -> "complete" pattern used elsewhere (e.g., profile image).
 */
@Getter
@Setter
@Schema(description = "Request payload for completing an event cover image upload (includes coverId)")
public class EventCoverImageCompleteRequest {

    @NotNull
    @Schema(description = "Cover object ID returned from the presigned upload response", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID coverId;

    @Valid
    @NotNull
    @Schema(description = "Upload completion payload (objectKey/resourceUrl + metadata)", requiredMode = Schema.RequiredMode.REQUIRED)
    private EventMediaUploadCompleteRequest upload;
}

