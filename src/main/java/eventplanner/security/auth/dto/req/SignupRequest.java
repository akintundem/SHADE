package eventplanner.security.auth.dto.req;

import eventplanner.common.domain.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @Schema(description = "User email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(description = "Display name for the user", example = "Ada Lovelace")
    @Size(min = 1, max = 120, message = "Name must be between 1 and 120 characters")
    private String name;

    @Schema(description = "Phone number in E.164 format", example = "+15551234567")
    @Size(max = 40, message = "Phone number must be 40 characters or fewer")
    private String phoneNumber;

    @Schema(description = "Whether the user opted in to marketing communications", example = "true")
    private Boolean marketingOptIn;

    @Schema(description = "Whether the user accepted terms of service", example = "true")
    private Boolean acceptTerms;

    @Schema(description = "Whether the user accepted the privacy policy", example = "true")
    private Boolean acceptPrivacy;

    @Schema(description = "Cognito subject to attach to this user (set after Cognito signup)", example = "c1b2c3d4-5678-90ab-cdef-1234567890ab")
    @Size(max = 120, message = "Cognito subject must be 120 characters or fewer")
    private String cognitoSub;

    @Schema(description = "User type classification", example = "INDIVIDUAL")
    private UserType userType;
}
