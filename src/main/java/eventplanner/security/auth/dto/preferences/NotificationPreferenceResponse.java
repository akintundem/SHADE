package eventplanner.security.auth.dto.preferences;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for notification preference
 */
@Data
public class NotificationPreferenceResponse {

    private UUID id;
    private String notificationType;
    private String channel;
    private Boolean enabled;
    private String frequency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
