package eventplanner.common.storage.s3;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

@Value
@Builder
public class PresignedUploadResult {
    String uploadMethod;
    String uploadUrl;
    Map<String, String> headers;
    String objectKey;
    String resourceUrl;
    LocalDateTime expiresAt;
}
