package eventplanner.common.communication.services.channel.push;

import eventplanner.common.communication.services.channel.push.dto.RegisterDeviceTokenRequest;
import eventplanner.common.communication.model.DeviceToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Push Notification operations
 */
@RestController
@RequestMapping("/api/v1/push-notifications")
@RequiredArgsConstructor
@Slf4j
public class PushNotificationController {

    private final DeviceTokenService deviceTokenService;

    /**
     * Register or update a device token for push notifications
     */
    @PostMapping("/devices/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DeviceToken> registerDeviceToken(
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        log.info("Registering device token for user: {}", request.getUserId());
        DeviceToken token = deviceTokenService.registerDeviceToken(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

}

