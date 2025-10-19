package ai.eventplanner.auth.controller;

import ai.eventplanner.auth.dto.UserSessionResponse;
import ai.eventplanner.auth.service.AuthService;
import ai.eventplanner.auth.service.UserPrincipal;
import ai.eventplanner.common.exception.UnauthorizedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/sessions")
public class SessionController {

    private final AuthService authService;

    public SessionController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public List<UserSessionResponse> listSessions(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return authService.getActiveSessions(principal.getUser());
    }

    @DeleteMapping("/all")
    public Map<String, Object> terminateAll(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        authService.terminateAllSessions(principal.getUser());
        return Map.of("message", "Sessions terminated");
    }
}
