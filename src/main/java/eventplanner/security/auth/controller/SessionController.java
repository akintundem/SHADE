package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.res.UserSessionResponse;
import eventplanner.security.auth.service.SessionManagementService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.dto.ApiMessageResponse;
import eventplanner.common.exception.UnauthorizedException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/sessions")
public class SessionController {

    private final SessionManagementService sessionManagementService;

    public SessionController(SessionManagementService sessionManagementService) {
        this.sessionManagementService = sessionManagementService;
    }

    @GetMapping
    public List<UserSessionResponse> listSessions(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return sessionManagementService.getActiveSessions(principal.getUser());
    }

    @DeleteMapping("/all")
    public ResponseEntity<ApiMessageResponse> terminateAll(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        sessionManagementService.terminateAllSessions(principal.getUser());
        return ResponseEntity.ok(ApiMessageResponse.success("Sessions terminated"));
    }
}
