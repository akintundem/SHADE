package eventplanner.common.communication.services.channel.push.dto;

import eventplanner.common.communication.model.DeviceToken;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for refreshing device tokens
 */
@Schema(description = "Request to refresh a device token for push notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshDeviceTokenRequest {
    
    @NotNull(message = "User ID is required")
    @Schema(description = "User ID associated with the device token", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;
    
    @NotBlank(message = "Device token is required")
    @Schema(description = "New device token to refresh", required = true, example = "fcm_token_string_here")
    private String deviceToken;
    
    @Schema(description = "Platform of the device (iOS, ANDROID)", example = "IOS")
    private DeviceToken.Platform platform;
    
    @Schema(description = "Device ID for identification", example = "device-uuid-123")
    private String deviceId;
    
    @Schema(description = "App version", example = "1.0.0")
    private String appVersion;
    
    @Schema(description = "Old device token to replace (optional, if not provided, will refresh most recent active token)", example = "old_fcm_token_string")
    private String oldDeviceToken;
}

