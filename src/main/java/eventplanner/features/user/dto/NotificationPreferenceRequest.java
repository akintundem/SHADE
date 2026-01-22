package eventplanner.features.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO for setting notification preference
 */
@Data
public class NotificationPreferenceRequest {

    @NotBlank(message = "Notification type is required")
    private String notificationType;

    @NotBlank(message = "Channel is required")
    @Pattern(regexp = "EMAIL|PUSH|SMS", message = "Channel must be EMAIL, PUSH, or SMS")
    private String channel;

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;

    @Pattern(regexp = "IMMEDIATE|DAILY_DIGEST|WEEKLY_DIGEST",
             message = "Frequency must be IMMEDIATE, DAILY_DIGEST, or WEEKLY_DIGEST")
    private String frequency;
}
