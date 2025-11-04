package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
@Schema(description = "Presigned upload response containing temporary upload credentials")
public class EventPresignedUploadResponse {

    @Schema(description = "Identifier that should be referenced once upload completes")
    UUID mediaId;

    @Schema(description = "HTTP method to use when uploading")
    String uploadMethod;

    @Schema(description = "URL to upload the media/asset to")
    String uploadUrl;

    @Schema(description = "Headers that must be included when uploading")
    Map<String, String> headers;

    @Schema(description = "URL where the media will be accessible after processing")
    String resourceUrl;

    @Schema(description = "Expiration timestamp for the presigned request")
    LocalDateTime expiresAt;
}
