package ai.eventplanner.auth.controller;

import ai.eventplanner.auth.dto.res.SecureUserResponse;
import ai.eventplanner.auth.dto.req.UpdateUserProfileRequest;
import ai.eventplanner.auth.service.UserAccountService;
import ai.eventplanner.auth.service.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * User Management Controller - Authorization handled by RBAC filter
 */
@RestController
@RequestMapping("/api/v1/auth/users")
public class UserManagementController {

    private final UserAccountService userAccountService;

    public UserManagementController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/{userId}")
    public SecureUserResponse getUser(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable UUID userId) {
        return userAccountService.getSecureUser(userId);
    }

    @PutMapping("/{userId}")
    public SecureUserResponse updateUser(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable UUID userId,
                                   @Valid @RequestBody UpdateUserProfileRequest request) {
        return userAccountService.updateSecureUser(userId, principal.getUser(), request);
    }

    @GetMapping("/search")
    public Page<SecureUserResponse> searchUsers(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam(defaultValue = "") String searchTerm,
                                          @PageableDefault(size = 10) Pageable pageable) {
        return userAccountService.searchSecureUsers(searchTerm, pageable);
    }
}
