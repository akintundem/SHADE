package eventplanner.security.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Combined request for onboarding data and profile image upload metadata")
public class CompleteOnboardingWithImageRequest {

    @Valid
    @NotNull(message = "Onboarding data is required")
    @Schema(description = "User profile information")
    private OnboardingRequest onboarding;

    @Valid
    @Schema(description = "Optional profile image upload metadata (to get a presigned URL)")
    private ProfileImageUploadRequest imageUpload;
}

