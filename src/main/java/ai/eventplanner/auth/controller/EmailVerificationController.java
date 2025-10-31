package ai.eventplanner.auth.controller;

import ai.eventplanner.auth.dto.req.ForgotPasswordRequest;
import ai.eventplanner.auth.service.AccountRecoveryService;
import ai.eventplanner.common.dto.ApiMessageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class EmailVerificationController {

    private final AccountRecoveryService accountRecoveryService;

    public EmailVerificationController(AccountRecoveryService accountRecoveryService) {
        this.accountRecoveryService = accountRecoveryService;
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiMessageResponse> resendVerification(
            @Valid @RequestBody ForgotPasswordRequest request) {
        String message = accountRecoveryService.resendVerification(request);
        return ResponseEntity.ok(ApiMessageResponse.success(message));
    }

    @GetMapping("/verify-email/{token}")
    public ResponseEntity<ApiMessageResponse> verifyEmail(@PathVariable String token) {
        boolean verified = accountRecoveryService.verifyEmailToken(token);
        ApiMessageResponse response = verified
            ? ApiMessageResponse.success("Email verified successfully")
            : ApiMessageResponse.failure("Invalid or expired verification token");

        return verified
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
