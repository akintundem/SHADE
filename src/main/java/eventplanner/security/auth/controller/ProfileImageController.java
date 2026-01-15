package eventplanner.security.auth.controller;

import eventplanner.common.exception.UnauthorizedException;
import eventplanner.security.auth.dto.req.ProfileImageCompleteRequest;
import eventplanner.security.auth.dto.req.ProfileImageUploadRequest;
import eventplanner.security.auth.dto.res.ProfileImageCompleteResponse;
import eventplanner.security.auth.dto.res.ProfileImageUploadResponse;
import eventplanner.security.auth.service.ProfileImageService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    public ProfileImageController(ProfileImageService profileImageService) {
        this.profileImageService = profileImageService;
    }

    @PostMapping("/profile-image/upload-url")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    public ResponseEntity<ProfileImageUploadResponse> createUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileImageUploadRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        ProfileImageUploadResponse response = profileImageService.createProfileImageUpload(principal.getUser(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/profile-image/complete")
    @RequiresPermission(value = RbacPermissions.USER_UPDATE, resources = {"user_id=#principal.id"})
    public ResponseEntity<ProfileImageCompleteResponse> completeUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileImageCompleteRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        ProfileImageCompleteResponse response = profileImageService.completeProfileImageUpload(principal.getUser(), request);
        return ResponseEntity.ok(response);
    }
}
