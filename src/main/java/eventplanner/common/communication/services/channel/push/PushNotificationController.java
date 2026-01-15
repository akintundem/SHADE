package eventplanner.common.communication.services.channel.push;

import eventplanner.common.communication.services.channel.push.dto.RefreshDeviceTokenRequest;
import eventplanner.common.communication.services.channel.push.dto.RegisterDeviceTokenRequest;
import eventplanner.common.communication.model.DeviceToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;

import java.util.UUID;

/**
 * REST Controller for Push Notification operations
 */
@Tag(name = "Push Notifications", description = "Push notification device token management operations")
@RestController
@RequestMapping("/api/v1/push-notifications")
@RequiredArgsConstructor
@Slf4j
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;

    /**
     * Register or update a device token for push notifications
     */
    @Operation(
            summary = "Register or update device token",
            description = "Register a new device token or update an existing one for push notifications"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Device token registered successfully",
                    content = @Content(schema = @Schema(implementation = DeviceToken.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            )
    })
    @PostMapping("/devices/register")
    @RequiresPermission(value = RbacPermissions.USER_NOTIFICATIONS_MANAGE, resources = {"user_id=#principal.id"})
    public ResponseEntity<DeviceToken> registerDeviceToken(
            @Valid @RequestBody RegisterDeviceTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = requireUserId(principal);
        request.setUserId(userId);
        log.info("Registering device token for user: {}", userId);
        DeviceToken token = pushNotificationService.registerDeviceToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @Operation(
            summary = "Refresh device token",
            description = "Refresh an existing device token with a new token value. Useful when tokens expire or are regenerated."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Device token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = DeviceToken.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or token not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            )
    })
    @PostMapping("/refresh-device-token")
    @RequiresPermission(value = RbacPermissions.USER_NOTIFICATIONS_MANAGE, resources = {"user_id=#principal.id"})
    public ResponseEntity<DeviceToken> refreshDeviceToken(
            @Valid @RequestBody RefreshDeviceTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = requireUserId(principal);
        request.setUserId(userId);
        log.info("Refreshing device token for user: {}", userId);
        DeviceToken token = pushNotificationService.refreshDeviceToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    private UUID requireUserId(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal.getId();
    }
}
