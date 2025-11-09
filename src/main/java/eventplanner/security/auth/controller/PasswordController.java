package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.req.ChangePasswordRequest;
import eventplanner.security.auth.dto.req.ForgotPasswordRequest;
import eventplanner.security.auth.dto.req.ResetPasswordRequest;
import eventplanner.security.auth.service.AccountRecoveryService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.common.dto.ApiMessageResponse;
import eventplanner.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class PasswordController {

    private final AccountRecoveryService accountRecoveryService;

    public PasswordController(AccountRecoveryService accountRecoveryService) {
        this.accountRecoveryService = accountRecoveryService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = accountRecoveryService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiMessageResponse.success(message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean changed = accountRecoveryService.resetPassword(request);
        ApiMessageResponse response = changed
                ? ApiMessageResponse.success("Password reset successfully")
                : ApiMessageResponse.failure("Unable to reset password");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    public ResponseEntity<ApiMessageResponse> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                           @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        accountRecoveryService.changePassword(principal.getUser(), request);
        return ResponseEntity.ok(ApiMessageResponse.success("Password changed successfully"));
    }
}
