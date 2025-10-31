package ai.eventplanner.auth.controller;

import ai.eventplanner.auth.dto.req.ChangePasswordRequest;
import ai.eventplanner.auth.dto.req.ForgotPasswordRequest;
import ai.eventplanner.auth.dto.req.ResetPasswordRequest;
import ai.eventplanner.auth.service.AuthService;
import ai.eventplanner.auth.service.UserPrincipal;
import ai.eventplanner.common.dto.ApiMessageResponse;
import ai.eventplanner.common.exception.UnauthorizedException;
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

    private final AuthService authService;

    public PasswordController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiMessageResponse.success(message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean changed = authService.resetPassword(request);
        ApiMessageResponse response = changed
                ? ApiMessageResponse.success("Password reset successfully")
                : ApiMessageResponse.failure("Unable to reset password");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiMessageResponse> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                           @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        authService.changePassword(principal.getUser(), request);
        return ResponseEntity.ok(ApiMessageResponse.success("Password changed successfully"));
    }
}
