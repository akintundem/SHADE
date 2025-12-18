package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.req.CompleteOnboardingWithImageRequest;
import eventplanner.security.auth.dto.res.CompleteOnboardingWithImageResponse;
import eventplanner.security.auth.dto.req.LoginRequest;
import eventplanner.security.auth.dto.req.LogoutRequest;
import eventplanner.security.auth.dto.req.OnboardingRequest;
import eventplanner.security.auth.dto.req.RefreshTokenRequest;
import eventplanner.security.auth.dto.req.RegisterRequest;
import eventplanner.security.auth.dto.res.SecureAuthResponse;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.dto.res.TokenValidationResponse;
import eventplanner.security.util.AuthMapper;
import eventplanner.security.auth.service.AuthService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.common.dto.ApiMessageResponse;
import eventplanner.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", "auth-service",
                "status", "healthy",
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiMessageResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        authService.register(request, resolveClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiMessageResponse.success("User registered successfully. Please check your email for verification.")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<SecureAuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        SecureAuthResponse response = authService.login(request, resolveClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @RequiresPermission(value = RbacPermissions.AUTH_ME, resources = {"user_id=#principal.id"})
    public ResponseEntity<SecureUserResponse> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return ResponseEntity.ok(AuthMapper.toSecureUserResponse(principal.getUser()));
    }

    @PostMapping("/refresh-token")
    @RequiresPermission(value = RbacPermissions.AUTH_REFRESH, resources = {"user_id=#principal.id"})
    public ResponseEntity<SecureAuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("User not found");
        }

        SecureAuthResponse response = authService.refreshToken(request, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestParam("token") String token) {
        TokenValidationResponse response = authService.validateToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @RequiresPermission(value = RbacPermissions.AUTH_LOGOUT, resources = {"user_id=#principal.id"})
    public ResponseEntity<ApiMessageResponse> logout(
            @Valid @RequestBody LogoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        if (principal == null) {
            throw new ResourceNotFoundException("User not found");
        }

        String deviceId = principal.getDeviceId();
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new ResourceNotFoundException("Device identifier is required");
        }

        authService.logout(request, principal.getUser(), deviceId.trim());
        return ResponseEntity.ok(ApiMessageResponse.success("Logged out successfully from this device"));
    }

    @PostMapping("/complete-onboarding")
    @RequiresPermission(value = RbacPermissions.AUTH_ME, resources = {"user_id=#principal.id"})
    public ResponseEntity<SecureUserResponse> completeOnboarding(
            @Valid @RequestBody OnboardingRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        if (principal == null) {
            throw new ResourceNotFoundException("User not found");
        }

        SecureUserResponse response = authService.completeOnboarding(
            request, 
            principal.getUser(), 
            resolveClientIp(httpRequest)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete-onboarding-with-image")
    @RequiresPermission(value = RbacPermissions.AUTH_ME, resources = {"user_id=#principal.id"})
    public ResponseEntity<CompleteOnboardingWithImageResponse> completeOnboardingWithImage(
            @Valid @RequestBody CompleteOnboardingWithImageRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        if (principal == null) {
            throw new ResourceNotFoundException("User not found");
        }

        CompleteOnboardingWithImageResponse response = authService.completeOnboardingWithImage(
            request, 
            principal.getUser(), 
            resolveClientIp(httpRequest)
        );
        return ResponseEntity.ok(response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
