package eventplanner.features.feeds.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
@Schema(description = "Presigned upload details")
public class PresignedUploadResponse {
    UUID mediaId;
    String objectKey;
    String uploadMethod;
    String uploadUrl;
    Map<String, String> headers;
    String resourceUrl;
    LocalDateTime expiresAt;
}


