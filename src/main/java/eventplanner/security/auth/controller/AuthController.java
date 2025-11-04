package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.req.LoginRequest;
import eventplanner.security.auth.dto.req.RefreshTokenRequest;
import eventplanner.security.auth.dto.req.RegisterRequest;
import eventplanner.security.auth.dto.res.SecureAuthResponse;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.dto.res.TokenValidationResponse;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.util.AuthMapper;
import eventplanner.security.auth.service.AuthService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.dto.ApiMessageResponse;
import eventplanner.common.exception.ResourceNotFoundException;
import eventplanner.security.util.JwtValidationUtil;
import io.jsonwebtoken.Claims;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtValidationUtil jwtValidationUtil;
    private final UserAccountRepository userAccountRepository;

    public AuthController(AuthService authService,
                          JwtValidationUtil jwtValidationUtil,
                          UserAccountRepository userAccountRepository) {
        this.authService = authService;
        this.jwtValidationUtil = jwtValidationUtil;
        this.userAccountRepository = userAccountRepository;
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
    public ResponseEntity<SecureAuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        SecureAuthResponse response = authService.register(request, resolveClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<SecureAuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        SecureAuthResponse response = authService.login(request, resolveClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<SecureUserResponse> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return ResponseEntity.ok(AuthMapper.toSecureUserResponse(principal.getUser()));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<SecureAuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        SecureAuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestParam("token") String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(TokenValidationResponse.invalid("Token is required"));
            }
            
            boolean valid = jwtValidationUtil.validateToken(token);
            if (!valid) {
                return ResponseEntity.ok(TokenValidationResponse.invalid(null));
            }
            
            Claims claims = jwtValidationUtil.getClaimsFromToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            return ResponseEntity.ok(TokenValidationResponse.valid(AuthMapper.toSecureUserResponse(user)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(TokenValidationResponse.invalid("Invalid token format"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiMessageResponse> logout(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null) {
            authService.logout(principal.getUser());
        }
        return ResponseEntity.ok(ApiMessageResponse.success("Logged out successfully"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
