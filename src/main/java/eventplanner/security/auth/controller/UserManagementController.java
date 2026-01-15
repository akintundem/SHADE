package eventplanner.security.auth.controller;

import eventplanner.features.feeds.dto.response.PostListResponse;
import eventplanner.security.auth.dto.req.NotificationSettingsUpdateRequest;
import eventplanner.security.auth.dto.req.PrivacySettingsUpdateRequest;
import eventplanner.security.auth.dto.req.SecuritySettingsUpdateRequest;
import eventplanner.security.auth.dto.req.UpdateUserProfileRequest;
import eventplanner.security.auth.dto.res.LocationSearchResponse;
import eventplanner.security.auth.dto.res.PublicUserResponse;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.service.LocationService;
import eventplanner.security.auth.service.UserAccountService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import jakarta.validation.Valid;
import eventplanner.common.dto.ApiMessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/users")
@Validated
@Tag(name = "User Management", description = "Endpoints for user management and location search")
public class UserManagementController {

    private final UserAccountService userAccountService;
    private final LocationService locationService;

    public UserManagementController(UserAccountService userAccountService, LocationService locationService) {
        this.userAccountService = userAccountService;
        this.locationService = locationService;
    }

    @GetMapping("/{userId}")
    @RequiresPermission(RbacPermissions.ADMIN_USERS_DETAIL)
    public SecureUserResponse getUser(@PathVariable UUID userId) {
        return userAccountService.getSecureUser(userId);
    }

    @PutMapping("/{userId}")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#userId"})
    public SecureUserResponse updateUser(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable UUID userId,
                                         @Valid @RequestBody UpdateUserProfileRequest request) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return userAccountService.updateSecureUser(userId, principal.getUser(), request);
    }

    @DeleteMapping("/{userId}")
    @RequiresPermission(value = RbacPermissions.USER_DELETE, resources = {"user_id=#userId"})
    public ResponseEntity<ApiMessageResponse> deleteUser(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable UUID userId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        userAccountService.deleteUserAccount(userId, principal.getUser());
        return ResponseEntity.ok(ApiMessageResponse.success("User account deleted"));
    }

    @GetMapping("/search")
    @RequiresPermission(RbacPermissions.ADMIN_USERS_READ)
    public Page<SecureUserResponse> searchUsers(@RequestParam(defaultValue = "") String searchTerm,
                                          @PageableDefault(size = 10) Pageable pageable) {
        String sanitizedTerm = searchTerm != null ? searchTerm.trim() : "";
        if (sanitizedTerm.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "searchTerm must be provided");
        }
        return userAccountService.searchSecureUsers(sanitizedTerm, pageable);
    }

    @GetMapping("/directory")
    @RequiresPermission(RbacPermissions.USER_SEARCH)
    public Page<PublicUserResponse> searchDirectory(@RequestParam(defaultValue = "") String searchTerm,
                                              @PageableDefault(size = 10) Pageable pageable) {
        String sanitizedTerm = searchTerm != null ? searchTerm.trim() : "";
        if (sanitizedTerm.isEmpty()) {
            return userAccountService.listPublicUsers(pageable);
        }
        return userAccountService.searchPublicUsers(sanitizedTerm, pageable);
    }

    /**
     * Search for cities in North America.
     * Supports searching by city name, state/province, or country.
     * Results are sorted with exact city name matches first.
     * 
     * @param query Search query (city, state, or country name)
     * @param pageable Pagination parameters (page, size, sort)
     * @return Paginated list of matching locations
     */
    @GetMapping("/locations/search")
    @RequiresPermission(RbacPermissions.USER_SEARCH)
    @Operation(
        summary = "Search locations",
        description = "Search for cities in North America (United States, Canada, Mexico). " +
                     "Supports searching by city name, state/province name, or country. " +
                     "Results are sorted with exact city name matches appearing first. " +
                     "Useful for dropdown/autocomplete functionality when users select their location. " +
                     "Supports pagination via page and size query parameters."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<Page<LocationSearchResponse>> searchLocations(
            @Parameter(description = "Search query (city, state, or country name)", example = "New York")
            @RequestParam(required = false)
            String query,
            @Parameter(description = "Pagination parameters (page, size, sort). Default size: 20, max size: 50")
            @PageableDefault(size = 20) Pageable pageable) {
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(Page.empty(pageable));
        }

        // Validate query length if provided
        String trimmedQuery = query.trim();
        if (trimmedQuery.length() > 100) {
            return ResponseEntity.badRequest().build();
        }

        Page<LocationSearchResponse> results = locationService.searchLocations(trimmedQuery, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/me/posts")
    @RequiresPermission(RbacPermissions.USER_SEARCH)
    @Operation(summary = "Get my posts", description = "Get all posts created by the current user across all events")
    public ResponseEntity<PostListResponse> getMyPosts(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        PostListResponse posts = 
                userAccountService.getUserPosts(principal, page, size);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{userId}/posts")
    @RequiresPermission(RbacPermissions.USER_SEARCH)
    @Operation(summary = "Get user posts", description = "Get all posts created by the requested user across all events")
    public ResponseEntity<PostListResponse> getUserPostsById(
            @PathVariable UUID userId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        ensurePrincipalMatchesUser(userId, principal);
        PostListResponse posts = userAccountService.getUserPosts(principal, page, size);
        return ResponseEntity.ok(posts);
    }

    @PutMapping("/me/notification-settings")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Update notification settings", description = "Update current user's notification preferences")
    public ResponseEntity<SecureUserResponse> updateNotificationSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationSettingsUpdateRequest request) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        SecureUserResponse updated = userAccountService.updateNotificationSettings(principal, request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{userId}/notification-settings")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#userId"})
    @Operation(summary = "Update notification settings", description = "Update the specified user's notification preferences")
    public ResponseEntity<SecureUserResponse> updateNotificationSettingsForUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationSettingsUpdateRequest request) {
        ensurePrincipalMatchesUser(userId, principal);
        SecureUserResponse updated = userAccountService.updateNotificationSettings(principal, request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/me/privacy-settings")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Update privacy settings", description = "Update current user's privacy preferences")
    public ResponseEntity<SecureUserResponse> updatePrivacySettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PrivacySettingsUpdateRequest request) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        SecureUserResponse updated = userAccountService.updatePrivacySettings(principal, request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{userId}/privacy-settings")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#userId"})
    @Operation(summary = "Update privacy settings", description = "Update the specified user's privacy preferences")
    public ResponseEntity<SecureUserResponse> updatePrivacySettingsForUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PrivacySettingsUpdateRequest request) {
        ensurePrincipalMatchesUser(userId, principal);
        SecureUserResponse updated = userAccountService.updatePrivacySettings(principal, request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/me/security-settings")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    @Operation(summary = "Update security settings", description = "Update current user's security preferences (MFA, etc.)")
    public ResponseEntity<SecureUserResponse> updateSecuritySettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SecuritySettingsUpdateRequest request) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        SecureUserResponse updated = userAccountService.updateSecuritySettings(principal, request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{userId}/security-settings")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#userId"})
    @Operation(summary = "Update security settings", description = "Update the specified user's security preferences")
    public ResponseEntity<SecureUserResponse> updateSecuritySettingsForUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SecuritySettingsUpdateRequest request) {
        ensurePrincipalMatchesUser(userId, principal);
        SecureUserResponse updated = userAccountService.updateSecuritySettings(principal, request);
        return ResponseEntity.ok(updated);
    }

    private void ensurePrincipalMatchesUser(UUID userId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (!principal.getId().equals(userId)) {
            throw new AccessDeniedException("Cannot access another user's data");
        }
    }

}
