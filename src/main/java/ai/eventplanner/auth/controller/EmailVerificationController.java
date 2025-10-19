package ai.eventplanner.auth.controller;

import ai.eventplanner.auth.dto.ForgotPasswordRequest;
import ai.eventplanner.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class EmailVerificationController {

    private final AuthService authService;

    public EmailVerificationController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/verify-email")
    public Map<String, Object> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = authService.resendVerification(request);
        return Map.of("message", message);
    }

    @GetMapping("/verify-email/{token}")
    public ResponseEntity<Map<String, Object>> verifyEmail(@PathVariable String token) {
        boolean verified = authService.verifyEmailToken(token);
        if (verified) {
            return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
        }
        return ResponseEntity.status(400).body(Map.of("message", "Invalid or expired verification token"));
    }
}
