package eventplanner.security.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for setting privacy setting
 */
@Data
public class PrivacySettingRequest {

    @NotBlank(message = "Setting key is required")
    @Size(max = 100, message = "Setting key must not exceed 100 characters")
    private String key;

    @NotBlank(message = "Setting value is required")
    @Pattern(regexp = "PUBLIC|PRIVATE|FRIENDS_ONLY",
             message = "Value must be PUBLIC, PRIVATE, or FRIENDS_ONLY")
    private String value;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
