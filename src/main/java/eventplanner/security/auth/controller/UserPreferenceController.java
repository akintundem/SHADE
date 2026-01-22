package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.preferences.UserPreferenceRequest;
import eventplanner.security.auth.dto.preferences.UserPreferenceResponse;
import eventplanner.security.auth.entity.UserPreference;
import eventplanner.security.auth.service.UserPreferenceService;
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

import java.util.Map;
import java.util.UUID;

/**
 * Controller for user preferences management
 * Uses /me pattern for security - users can only access their own preferences
 */
@RestController
@RequestMapping("/api/v1/users/me/preferences")
@Tag(name = "User Preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    @GetMapping
    @RequiresPermission(value = RbacPermissions.USER_READ, resources = {"user_id=#principal.id"})
    @Operation(summary = "Get my preferences", description = "Get all preferences for the authenticated user as a key-value map")
    public ResponseEntity<Map<String, String>> getMyPreferences(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        Map<String, String> preferences = preferenceService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @GetMapping("/{key}")
    @RequiresPermission(value = RbacPermissions.USER_READ, resources = {"user_id=#principal.id"})
    @Operation(summary = "Get specific preference", description = "Get a specific preference value by key")
    public ResponseEntity<String> getMyPreference(
        @PathVariable String key,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        return preferenceService.getUserPreference(userId, key)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Set user preference", description = "Set or update a user preference")
    public ResponseEntity<UserPreferenceResponse> setPreference(
        @Valid @RequestBody UserPreferenceRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        UserPreference preference = preferenceService.setUserPreference(
            userId,
            request.getKey(),
            request.getValue(),
            request.getDescription()
        );

        return ResponseEntity.ok(toResponse(preference));
    }

    @PutMapping("/batch")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Set multiple preferences", description = "Set or update multiple preferences at once")
    public ResponseEntity<Void> setPreferences(
        @RequestBody Map<String, String> preferences,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        preferenceService.setUserPreferences(userId, preferences);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{key}")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Delete preference", description = "Delete a specific user preference")
    public ResponseEntity<Void> deletePreference(
        @PathVariable String key,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        preferenceService.deleteUserPreference(userId, key);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Delete all preferences", description = "Delete all preferences for the authenticated user")
    public ResponseEntity<Void> deleteAllPreferences(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        preferenceService.deleteAllUserPreferences(userId);
        return ResponseEntity.noContent().build();
    }

    private UserPreferenceResponse toResponse(UserPreference entity) {
        UserPreferenceResponse response = new UserPreferenceResponse();
        response.setId(entity.getId());
        response.setKey(entity.getPreferenceKey());
        response.setValue(entity.getPreferenceValue());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
