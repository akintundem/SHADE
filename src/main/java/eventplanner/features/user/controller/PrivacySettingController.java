package eventplanner.features.user.controller;

import eventplanner.features.user.dto.PrivacySettingRequest;
import eventplanner.features.user.dto.PrivacySettingResponse;
import eventplanner.features.user.entity.UserPrivacySetting;
import eventplanner.features.user.service.UserPrivacySettingService;
import eventplanner.security.auth.service.UserPrincipal;
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
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/privacy-settings")
@Tag(name = "Privacy Settings")
@RequiredArgsConstructor
public class PrivacySettingController {

    private final UserPrivacySettingService privacySettingService;

    @GetMapping
    @Operation(summary = "Get all privacy settings", description = "Get all privacy settings for a user as a key-value map")
    public ResponseEntity<Map<String, String>> getPrivacySettings(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        Map<String, String> settings = privacySettingService.getUserPrivacySettings(userId);
        return ResponseEntity.ok(settings);
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get specific privacy setting", description = "Get a specific privacy setting value by key")
    public ResponseEntity<String> getPrivacySetting(
        @PathVariable UUID userId,
        @PathVariable String key,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        return privacySettingService.getPrivacySetting(userId, key)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    @Operation(summary = "Set privacy setting", description = "Set or update a privacy setting")
    public ResponseEntity<PrivacySettingResponse> setPrivacySetting(
        @PathVariable UUID userId,
        @Valid @RequestBody PrivacySettingRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);

        UserPrivacySetting setting = privacySettingService.setPrivacySetting(
            userId,
            request.getKey(),
            request.getValue(),
            request.getDescription()
        );

        return ResponseEntity.ok(toResponse(setting));
    }

    @PutMapping("/batch")
    @Operation(summary = "Set multiple privacy settings", description = "Set or update multiple privacy settings at once")
    public ResponseEntity<Void> setPrivacySettings(
        @PathVariable UUID userId,
        @RequestBody Map<String, String> settings,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        privacySettingService.setPrivacySettings(userId, settings);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/make-public")
    @Operation(summary = "Make profile public", description = "Set all privacy settings to PUBLIC")
    public ResponseEntity<Void> makeProfilePublic(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        privacySettingService.makeProfilePublic(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/make-private")
    @Operation(summary = "Make profile private", description = "Set all privacy settings to PRIVATE")
    public ResponseEntity<Void> makeProfilePrivate(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        privacySettingService.makeProfilePrivate(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Delete privacy setting", description = "Delete a specific privacy setting")
    public ResponseEntity<Void> deletePrivacySetting(
        @PathVariable UUID userId,
        @PathVariable String key,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        privacySettingService.deletePrivacySetting(userId, key);
        return ResponseEntity.noContent().build();
    }

    private void validateUserAccess(UUID userId, UserPrincipal principal) {
        if (!principal.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "You can only access your own privacy settings"
            );
        }
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
