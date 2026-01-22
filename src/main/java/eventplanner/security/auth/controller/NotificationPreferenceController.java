package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.NotificationPreferenceRequest;
import eventplanner.security.auth.dto.NotificationPreferenceResponse;
import eventplanner.security.auth.entity.UserNotificationPreference;
import eventplanner.security.auth.service.UserNotificationPreferenceService;
import eventplanner.security.auth.service.UserPrincipal;
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
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/notification-preferences")
@Tag(name = "Notification Preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final UserNotificationPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Get all notification preferences", description = "Get all notification preferences for a user")
    public ResponseEntity<List<NotificationPreferenceResponse>> getNotificationPreferences(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);

        List<UserNotificationPreference> preferences = preferenceService.getUserNotificationPreferences(userId);
        List<NotificationPreferenceResponse> responses = preferences.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/check")
    @Operation(summary = "Check if notification is enabled", description = "Check if a specific notification type/channel is enabled")
    public ResponseEntity<Boolean> isNotificationEnabled(
        @PathVariable UUID userId,
        @RequestParam String notificationType,
        @RequestParam String channel,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        boolean enabled = preferenceService.isNotificationEnabled(userId, notificationType, channel);
        return ResponseEntity.ok(enabled);
    }

    @PutMapping
    @Operation(summary = "Set notification preference", description = "Set or update a notification preference")
    public ResponseEntity<NotificationPreferenceResponse> setNotificationPreference(
        @PathVariable UUID userId,
        @Valid @RequestBody NotificationPreferenceRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);

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
    @Operation(summary = "Enable all notifications", description = "Enable all notification preferences for the user")
    public ResponseEntity<Void> enableAllNotifications(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        preferenceService.enableAllNotifications(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/disable-all")
    @Operation(summary = "Disable all notifications", description = "Disable all notification preferences for the user")
    public ResponseEntity<Void> disableAllNotifications(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        preferenceService.disableAllNotifications(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @Operation(summary = "Delete specific notification preference", description = "Delete a notification preference by type and channel")
    public ResponseEntity<Void> deleteNotificationPreference(
        @PathVariable UUID userId,
        @RequestParam String notificationType,
        @RequestParam String channel,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        preferenceService.deleteNotificationPreference(userId, notificationType, channel);
        return ResponseEntity.noContent().build();
    }

    private void validateUserAccess(UUID userId, UserPrincipal principal) {
        if (!principal.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "You can only access your own notification preferences"
            );
        }
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
