package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.preferences.PrivacySettingRequest;
import eventplanner.security.auth.dto.preferences.PrivacySettingResponse;
import eventplanner.security.auth.entity.UserPrivacySetting;
import eventplanner.security.auth.service.UserPrivacySettingService;
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
 * Controller for privacy settings management
 * Uses /me pattern for security - users can only access their own privacy settings
 */
@RestController
@RequestMapping("/api/v1/users/me/privacy-settings")
@Tag(name = "Privacy Settings")
@RequiredArgsConstructor
public class PrivacySettingController {

    private final UserPrivacySettingService privacySettingService;

    @GetMapping
    @RequiresPermission(value = RbacPermissions.USER_READ, resources = {"user_id=#principal.id"})
    @Operation(summary = "Get my privacy settings", description = "Get all privacy settings for the authenticated user as a key-value map")
    public ResponseEntity<Map<String, String>> getMyPrivacySettings(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        Map<String, String> settings = privacySettingService.getUserPrivacySettings(userId);
        return ResponseEntity.ok(settings);
    }

    @GetMapping("/{key}")
    @RequiresPermission(value = RbacPermissions.USER_READ, resources = {"user_id=#principal.id"})
    @Operation(summary = "Get specific privacy setting", description = "Get a specific privacy setting value by key")
    public ResponseEntity<String> getMyPrivacySetting(
        @PathVariable String key,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        return privacySettingService.getPrivacySetting(userId, key)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Set privacy setting", description = "Set or update a privacy setting")
    public ResponseEntity<PrivacySettingResponse> setPrivacySetting(
        @Valid @RequestBody PrivacySettingRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        UserPrivacySetting setting = privacySettingService.setPrivacySetting(
            userId,
            request.getKey(),
            request.getValue(),
            request.getDescription()
        );

        return ResponseEntity.ok(toResponse(setting));
    }

    @PutMapping("/batch")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Set multiple privacy settings", description = "Set or update multiple privacy settings at once")
    public ResponseEntity<Void> setPrivacySettings(
        @RequestBody Map<String, String> settings,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        privacySettingService.setPrivacySettings(userId, settings);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/make-public")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Make profile public", description = "Set all privacy settings to PUBLIC")
    public ResponseEntity<Void> makeProfilePublic(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        privacySettingService.makeProfilePublic(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/make-private")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Make profile private", description = "Set all privacy settings to PRIVATE")
    public ResponseEntity<Void> makeProfilePrivate(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        privacySettingService.makeProfilePrivate(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{key}")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Delete privacy setting", description = "Delete a specific privacy setting")
    public ResponseEntity<Void> deletePrivacySetting(
        @PathVariable String key,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getUser().getId();
        privacySettingService.deletePrivacySetting(userId, key);
        return ResponseEntity.noContent().build();
    }

    private PrivacySettingResponse toResponse(UserPrivacySetting entity) {
        PrivacySettingResponse response = new PrivacySettingResponse();
        response.setId(entity.getId());
        response.setKey(entity.getSettingKey());
        response.setValue(entity.getSettingValue());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
