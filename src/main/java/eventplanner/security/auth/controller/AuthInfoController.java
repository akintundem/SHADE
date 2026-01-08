package eventplanner.security.auth.controller;

import eventplanner.common.dto.ApiMessageResponse;
import eventplanner.common.exception.ResourceNotFoundException;
import eventplanner.common.exception.UnauthorizedException;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.service.CognitoUserService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.util.AuthMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthInfoController {

    private final CognitoUserService cognitoUserService;

    public AuthInfoController(CognitoUserService cognitoUserService) {
        this.cognitoUserService = cognitoUserService;
    }

    @GetMapping("/me")
    @RequiresPermission(value = RbacPermissions.AUTH_ME, resources = {"user_id=#principal.id"})
    public ResponseEntity<SecureUserResponse> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return ResponseEntity.status(HttpStatus.OK).body(AuthMapper.toSecureUserResponse(principal.getUser()));
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
}
