package ai.eventplanner.auth.controller;

import ai.eventplanner.auth.dto.ChangePasswordRequest;
import ai.eventplanner.auth.dto.ForgotPasswordRequest;
import ai.eventplanner.auth.dto.PasswordResponse;
import ai.eventplanner.auth.dto.ResetPasswordRequest;
import ai.eventplanner.auth.service.AuthService;
import ai.eventplanner.auth.service.UserPrincipal;
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
    public ResponseEntity<PasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = authService.requestPasswordReset(request);
        PasswordResponse response = PasswordResponse.builder()
                .message(message)
                .success(true)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean changed = authService.resetPassword(request);
        PasswordResponse response = PasswordResponse.builder()
                .message(changed ? "Password reset successfully" : "Unable to reset password")
                .success(changed)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<PasswordResponse> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                           @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        authService.changePassword(principal.getUser(), request);
        PasswordResponse response = PasswordResponse.builder()
                .message("Password changed successfully")
                .success(true)
                .build();
        return ResponseEntity.ok(response);
    }
}
