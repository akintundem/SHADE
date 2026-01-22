package eventplanner.features.user.controller;

import eventplanner.features.user.dto.UserPreferenceRequest;
import eventplanner.features.user.dto.UserPreferenceResponse;
import eventplanner.features.user.entity.UserPreference;
import eventplanner.features.user.service.UserPreferenceService;
import eventplanner.security.auth.service.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for user preferences management
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
@Tag(name = "User Preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Get all user preferences", description = "Get all preferences for a user as a key-value map")
    public ResponseEntity<Map<String, String>> getUserPreferences(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        Map<String, String> preferences = preferenceService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get specific preference", description = "Get a specific preference value by key")
    public ResponseEntity<String> getUserPreference(
        @PathVariable UUID userId,
        @PathVariable String key,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        return preferenceService.getUserPreference(userId, key)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    @Operation(summary = "Set user preference", description = "Set or update a user preference")
    public ResponseEntity<UserPreferenceResponse> setPreference(
        @PathVariable UUID userId,
        @Valid @RequestBody UserPreferenceRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);

        UserPreference preference = preferenceService.setUserPreference(
            userId,
            request.getKey(),
            request.getValue(),
            request.getDescription()
        );

        return ResponseEntity.ok(toResponse(preference));
    }

    @PutMapping("/batch")
    @Operation(summary = "Set multiple preferences", description = "Set or update multiple preferences at once")
    public ResponseEntity<Void> setPreferences(
        @PathVariable UUID userId,
        @RequestBody Map<String, String> preferences,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        preferenceService.setUserPreferences(userId, preferences);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Delete preference", description = "Delete a specific user preference")
    public ResponseEntity<Void> deletePreference(
        @PathVariable UUID userId,
        @PathVariable String key,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        preferenceService.deleteUserPreference(userId, key);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Delete all preferences", description = "Delete all preferences for a user")
    public ResponseEntity<Void> deleteAllPreferences(
        @PathVariable UUID userId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        preferenceService.deleteAllUserPreferences(userId);
        return ResponseEntity.noContent().build();
    }

    private void validateUserAccess(UUID userId, UserPrincipal principal) {
        if (!principal.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "You can only access your own preferences"
            );
        }
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
