package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.req.SignupRequest;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.service.UserAccountService;
import eventplanner.security.util.AuthMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lightweight signup endpoint for Cognito flows.
 * Creates/updates a local user record so downstream APIs can resolve the user by email/sub.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class SignupController {

    private final UserAccountService userAccountService;

    public SignupController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SecureUserResponse> signup(@Valid @RequestBody SignupRequest request) {
        try {
            var result = userAccountService.provisionCognitoUser(request);
            HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status).body(AuthMapper.toSecureUserResponse(result.user()));
        } catch (IllegalStateException ex) {
            // Return a generic error to avoid revealing whether an account exists
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credentials", ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credentials", ex);
        }
    }
}
