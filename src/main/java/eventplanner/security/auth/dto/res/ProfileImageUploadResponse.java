package eventplanner.security.auth.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

@Value
@Builder
@Schema(description = "Presigned upload details for updating a user's profile image")
public class ProfileImageUploadResponse {

    @Schema(description = "HTTP method to use when uploading", example = "PUT")
    String uploadMethod;

    @Schema(description = "Pre-signed URL to upload the image to S3")
    String uploadUrl;

    @Schema(description = "Headers that must be included when uploading")
    Map<String, String> headers;

    @Schema(description = "Key of the object that will be created in S3")
    String objectKey;

    @Schema(description = "URL where the uploaded image will be accessible")
    String resourceUrl;

    @Schema(description = "Expiration timestamp for the upload URL")
    LocalDateTime expiresAt;
}
