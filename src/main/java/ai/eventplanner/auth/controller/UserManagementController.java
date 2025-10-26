package ai.eventplanner.auth.controller;

import ai.eventplanner.auth.dto.UpdateUserProfileRequest;
import ai.eventplanner.auth.dto.UserResponse;
import ai.eventplanner.auth.service.AuthService;
import ai.eventplanner.auth.service.UserPrincipal;
import ai.eventplanner.common.exception.ForbiddenException;
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

@RestController
@RequestMapping("/api/v1/auth/users")
public class UserManagementController {

    private final AuthService authService;

    public UserManagementController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable UUID userId) {
        ensureSameUser(principal, userId);
        return authService.getUser(userId);
    }

    @PutMapping("/{userId}")
    public UserResponse updateUser(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable UUID userId,
                                   @Valid @RequestBody UpdateUserProfileRequest request) {
        ensureSameUser(principal, userId);
        return authService.updateUser(userId, principal.getUser(), request);
    }

    @GetMapping("/search")
    public Page<UserResponse> searchUsers(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam(defaultValue = "") String searchTerm,
                                          @PageableDefault(size = 10) Pageable pageable) {
        if (principal == null) {
            throw new ForbiddenException("Unauthorized");
        }
        throw new ForbiddenException("User search is restricted to administrators only");
    }

    private void ensureSameUser(UserPrincipal principal, UUID userId) {
        if (principal == null || !principal.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not allowed to access this resource");
        }
    }
}
