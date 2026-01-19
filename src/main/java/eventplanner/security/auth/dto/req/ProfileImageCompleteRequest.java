package eventplanner.security.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Sent after the client uploads the profile image to S3 using a presigned URL")
public class ProfileImageCompleteRequest {

    @NotBlank(message = "objectKey is required")
    @Schema(description = "S3 object key returned by /profile-image/upload-url")
    private String objectKey;

    @NotBlank(message = "resourceUrl is required")
    @Schema(description = "Non-presigned URL to the uploaded object (no query params). Use resourceUrl from /profile-image/upload-url.")
    private String resourceUrl;
}


