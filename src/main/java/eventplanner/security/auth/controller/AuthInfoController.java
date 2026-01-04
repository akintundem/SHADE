package eventplanner.security.auth.controller;

import eventplanner.common.exception.ResourceNotFoundException;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.util.AuthMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthInfoController {

    @GetMapping("/me")
    @RequiresPermission(value = RbacPermissions.AUTH_ME, resources = {"user_id=#principal.id"})
    public ResponseEntity<SecureUserResponse> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return ResponseEntity.status(HttpStatus.OK).body(AuthMapper.toSecureUserResponse(principal.getUser()));
    }
}
