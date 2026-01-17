package eventplanner.security.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @Schema(description = "Public handle/username", example = "ada.lovelace", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9](?:[A-Za-z0-9._]{1,28}[A-Za-z0-9])?$",
            message = "Username must be 3-30 characters and contain only letters, numbers, '.' or '_' (cannot start/end with '.' or '_')"
    )
    private String username;

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
}
