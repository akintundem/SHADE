package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.req.UpdateUserProfileRequest;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.service.UserAccountService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/v1/auth/users")
public class UserManagementController {

    private final UserAccountService userAccountService;

    public UserManagementController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/{userId}")
    @RequiresPermission(value = RbacPermissions.USER_READ, resources = {"user_id=#userId"})
    public SecureUserResponse getUser(@PathVariable UUID userId) {
        return userAccountService.getSecureUser(userId);
    }

    @PutMapping("/{userId}")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#userId"})
    public SecureUserResponse updateUser(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable UUID userId,
                                   @Valid @RequestBody UpdateUserProfileRequest request) {
        return userAccountService.updateSecureUser(userId, principal.getUser(), request);
    }

    @GetMapping("/search")
    @RequiresPermission(RbacPermissions.USER_SEARCH)
    public Page<SecureUserResponse> searchUsers(@RequestParam(defaultValue = "") String searchTerm,
                                          @PageableDefault(size = 10) Pageable pageable) {
        String sanitizedTerm = searchTerm != null ? searchTerm.trim() : "";
        if (sanitizedTerm.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "searchTerm must be provided");
        }
        return userAccountService.searchSecureUsers(sanitizedTerm, pageable);
    }
}
