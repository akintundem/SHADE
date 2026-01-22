package eventplanner.security.auth.dto.preferences;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for setting user preference
 */
@Data
public class UserPreferenceRequest {

    @NotBlank(message = "Preference key is required")
    @Size(max = 100, message = "Preference key must not exceed 100 characters")
    private String key;

    @Size(max = 500, message = "Preference value must not exceed 500 characters")
    private String value;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
