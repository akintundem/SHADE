package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.req.UpdateUserProfileRequest;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.service.UserAccountService;
import eventplanner.security.auth.service.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * User Management Controller with explicit self/admin guard to complement RBAC.
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
        assertSelfOrSystemAdmin(principal, userId);
        return userAccountService.getSecureUser(userId);
    }

    @PutMapping("/{userId}")
    public SecureUserResponse updateUser(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable UUID userId,
                                   @Valid @RequestBody UpdateUserProfileRequest request) {
        assertSelfOrSystemAdmin(principal, userId);
        return userAccountService.updateSecureUser(userId, principal.getUser(), request);
    }

    @GetMapping("/search")
    public Page<SecureUserResponse> searchUsers(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam(defaultValue = "") String searchTerm,
                                          @PageableDefault(size = 10) Pageable pageable) {
        String sanitizedTerm = searchTerm != null ? searchTerm.trim() : "";
        if (sanitizedTerm.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "searchTerm must be provided");
        }
        return userAccountService.searchSecureUsers(sanitizedTerm, pageable);
    }

    private void assertSelfOrSystemAdmin(UserPrincipal principal, UUID userId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (!userId.equals(principal.getId()) && !principal.isSystemAdmin()) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
