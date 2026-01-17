package eventplanner.security.auth.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUserProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[^<>]*$", message = "Name contains invalid characters")
    private String name;

    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9](?:[A-Za-z0-9._]{1,28}[A-Za-z0-9])?$",
            message = "Username must be 3-30 characters and contain only letters, numbers, '.' or '_' (cannot start/end with '.' or '_')"
    )
    private String username;

    @Pattern(regexp = "^\\+?[0-9 .\\-]{7,20}$", message = "Phone number must be valid")
    private String phoneNumber;

    @Size(max = 500)
    private String profilePictureUrl;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Boolean acceptTerms;

    private Boolean acceptPrivacy;

    @Size(max = 2000)
    private String preferences;

    private Boolean marketingOptIn;

    @Valid
    private UserSettingsUpdateRequest settings;

    @Size(max = 120)
    private String deviceId;
}
