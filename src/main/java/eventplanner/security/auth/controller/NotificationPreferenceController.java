package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.preferences.NotificationPreferenceRequest;
import eventplanner.security.auth.dto.preferences.NotificationPreferenceResponse;
import eventplanner.security.auth.entity.UserNotificationPreference;
import eventplanner.security.auth.service.UserNotificationPreferenceService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for notification preferences management
 * Uses /me pattern for security - users can only access their own notification preferences
 */
@RestController
@RequestMapping("/api/v1/users/me/notification-preferences")
@Tag(name = "Notification Preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final UserNotificationPreferenceService preferenceService;

    @GetMapping
    @RequiresPermission(value = RbacPermissions.USER_READ, resources = {"user_id=#principal.id"})
    @Operation(summary = "Get my notification preferences", description = "Get all notification preferences for the authenticated user")
    public ResponseEntity<List<NotificationPreferenceResponse>> getMyNotificationPreferences(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        List<UserNotificationPreference> preferences = preferenceService.getUserNotificationPreferences(userId);
        List<NotificationPreferenceResponse> responses = preferences.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/check")
    @RequiresPermission(value = RbacPermissions.USER_READ, resources = {"user_id=#principal.id"})
    @Operation(summary = "Check if notification is enabled", description = "Check if a specific notification type/channel is enabled")
    public ResponseEntity<Boolean> isNotificationEnabled(
        @RequestParam String notificationType,
        @RequestParam String channel,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        boolean enabled = preferenceService.isNotificationEnabled(userId, notificationType, channel);
        return ResponseEntity.ok(enabled);
    }

    @PutMapping
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Set notification preference", description = "Set or update a notification preference")
    public ResponseEntity<NotificationPreferenceResponse> setNotificationPreference(
        @Valid @RequestBody NotificationPreferenceRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        UserNotificationPreference preference = preferenceService.setNotificationPreference(
            userId,
            request.getNotificationType(),
            request.getChannel(),
            request.getEnabled(),
            request.getFrequency()
        );

        return ResponseEntity.ok(toResponse(preference));
    }

    @PostMapping("/enable-all")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Enable all notifications", description = "Enable all notification preferences for the authenticated user")
    public ResponseEntity<Void> enableAllNotifications(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        preferenceService.enableAllNotifications(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/disable-all")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Disable all notifications", description = "Disable all notification preferences for the authenticated user (global opt-out)")
    public ResponseEntity<Void> disableAllNotifications(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        preferenceService.disableAllNotifications(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Delete specific notification preference", description = "Delete a notification preference by type and channel")
    public ResponseEntity<Void> deleteNotificationPreference(
        @RequestParam String notificationType,
        @RequestParam String channel,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        preferenceService.deleteNotificationPreference(userId, notificationType, channel);
        return ResponseEntity.noContent().build();
    }

    private NotificationPreferenceResponse toResponse(UserNotificationPreference entity) {
        NotificationPreferenceResponse response = new NotificationPreferenceResponse();
        response.setId(entity.getId());
        response.setNotificationType(entity.getNotificationType());
        response.setChannel(entity.getChannel());
        response.setEnabled(entity.getEnabled());
        response.setFrequency(entity.getFrequency());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
