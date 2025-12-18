package eventplanner.security.auth.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing updated user info and optional presigned upload URL")
public class CompleteOnboardingWithImageResponse {

    @Schema(description = "Updated user profile info")
    private SecureUserResponse user;

    @Schema(description = "Presigned URL and metadata for profile image upload (if requested)")
    private ProfileImageUploadResponse imageUpload;
}

