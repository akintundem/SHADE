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
 * Request DTO for registering device tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceTokenRequest {

    @Schema(description = "User ID associated with the device token", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @NotBlank(message = "Device token is required")
    @Schema(description = "FCM device token", requiredMode = Schema.RequiredMode.REQUIRED, example = "fcm_token_string_here")
    private String deviceToken;

    @NotNull(message = "Platform is required")
    @Schema(description = "Platform of the device (IOS, ANDROID)", requiredMode = Schema.RequiredMode.REQUIRED, example = "IOS")
    private DeviceToken.Platform platform;

    @Schema(description = "Client-provided device identifier", example = "device-uuid-123")
    private String deviceId;

    @Schema(description = "App version running on the device", example = "1.0.0")
    private String appVersion;
}
