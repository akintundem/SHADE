package eventplanner.common.storage.s3.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Generic response for presigned upload requests
 */
@Value
@Builder
public class PresignedUploadResponse {
    UUID mediaId;
    String objectKey;
    String uploadMethod;
    String uploadUrl;
    Map<String, String> headers;
    String resourceUrl;
    LocalDateTime expiresAt;
}
