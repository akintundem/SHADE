package eventplanner.security.auth.dto.req;

import eventplanner.common.domain.enums.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[^<>]*$", message = "Name contains invalid characters")
    private String name;

    @Pattern(regexp = "^\\+?[0-9 .\\-]{7,20}$", message = "Phone number must be valid")
    private String phoneNumber;

    @Size(max = 500)
    private String profileImageUrl;

    private UserType userType = UserType.INDIVIDUAL;

    @Size(max = 2000)
    private String preferences;

    private Boolean marketingOptIn = Boolean.FALSE;

    @Size(max = 120)
    private String deviceId;

    @Size(max = 120)
    private String clientId;
}
