package eventplanner.security.auth.controller;

import eventplanner.common.dto.ApiMessageResponse;
import eventplanner.common.exception.ResourceNotFoundException;
import eventplanner.common.exception.UnauthorizedException;
import eventplanner.security.auth.dto.req.SignupRequest;
import eventplanner.security.auth.dto.res.AuthSessionResponse;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.service.CognitoUserService;
import eventplanner.security.auth.service.UserAccountService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.auth.dto.AuthMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthInfoController {

    private final CognitoUserService cognitoUserService;
    private final UserAccountService userAccountService;

    public AuthInfoController(CognitoUserService cognitoUserService, UserAccountService userAccountService) {
        this.cognitoUserService = cognitoUserService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/session")
    @RequiresPermission(value = RbacPermissions.AUTH_ME, resources = {"user_id=#principal.id"})
    public ResponseEntity<AuthSessionResponse> session(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("User not found");
        }
        var account = principal.getUser();
        var userResponse = AuthMapper.toSecureUserResponse(account);
        var session = AuthSessionResponse.builder()
                .user(userResponse)
                .onboardingRequired(isOnboardingRequired(account))
                .build();
        return ResponseEntity.ok(session);
    }

    @RequestMapping(value = "/logout", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<ApiMessageResponse> logout(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        cognitoUserService.signOutUser(
                principal.getUser().getCognitoSub(),
                principal.getUser().getEmail()
        );
        return ResponseEntity.ok(ApiMessageResponse.success("Logged out successfully"));
    }

    /**
     * Lightweight signup endpoint for Cognito flows.
     * Creates/updates a local user record so downstream APIs can resolve the user by email/sub.
     */
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

    private boolean isOnboardingRequired(UserAccount user) {
        if (user == null) {
            return true;
        }
        boolean missingName = !StringUtils.hasText(user.getName());
        boolean missingUsername = !StringUtils.hasText(user.getUsername());
        boolean missingPhone = !StringUtils.hasText(user.getPhoneNumber());
        boolean missingTerms = !user.isAcceptTerms();
        boolean missingPrivacy = !user.isAcceptPrivacy();
        return missingName || missingUsername || missingPhone || missingTerms || missingPrivacy;
    }
}
