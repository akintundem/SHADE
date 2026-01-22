package eventplanner.security.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user preference
 */
@Data
public class UserPreferenceResponse {

    private UUID id;
    private String key;
    private String value;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
