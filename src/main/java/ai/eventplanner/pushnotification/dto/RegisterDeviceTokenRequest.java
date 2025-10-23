package ai.eventplanner.pushnotification.dto;

import ai.eventplanner.comms.model.DeviceToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for registering device tokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceTokenRequest {
    
    private UUID userId;
    private String deviceToken;
    private DeviceToken.Platform platform;
    private String deviceId;
    private String appVersion;
}
