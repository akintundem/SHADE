package eventplanner.security.auth.controller;

import eventplanner.common.dto.ApiMessageResponse;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ForbiddenException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.common.exception.exceptions.UnauthorizedException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static eventplanner.security.util.AuthValidationUtil.normalizeEmail;
import static eventplanner.security.util.AuthValidationUtil.safeTrim;

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
    public ResponseEntity<SecureUserResponse> signup(@Valid @RequestBody SignupRequest request,
                                                     Authentication authentication) {
        try {
            Jwt jwt = requireJwt(authentication);
            String tokenUse = jwt.getClaimAsString("token_use");
            if (!"access".equals(tokenUse)) {
                throw new UnauthorizedException("Invalid token");
            }
            String tokenEmail = resolveTokenEmail(jwt);
            if (!isEmailVerified(jwt)) {
                throw new ForbiddenException("Email not verified");
            }

            String requestEmail = normalizeEmail(request.getEmail());
            if (!StringUtils.hasText(tokenEmail)) {
                if (!StringUtils.hasText(requestEmail)) {
                    throw new UnauthorizedException("Invalid token");
                }
                tokenEmail = requestEmail;
            }
            if (!tokenEmail.equalsIgnoreCase(requestEmail)) {
                throw new BadRequestException("Invalid signup payload");
            }

            request.setEmail(tokenEmail);
            String subject = jwt.getSubject();
            if (!StringUtils.hasText(subject)) {
                throw new UnauthorizedException("Invalid token");
            }
            var result = userAccountService.provisionCognitoUser(request, subject);
            UserAccount user = result.user();
            cognitoUserService.updateUserProfile(
                    subject,
                    tokenEmail,
                    user.getName(),
                    user.getUsername(),
                    user.getPhoneNumber()
            );
            HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status).body(AuthMapper.toSecureUserResponse(user));
        } catch (IllegalStateException ex) {
            // Return a generic error to avoid revealing whether an account exists
            throw new BadRequestException("Invalid credentials", ex);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid credentials", ex);
        }
    }

    private Jwt requireJwt(Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        Object details = authentication.getDetails();
        if (details instanceof Jwt jwt) {
            return jwt;
        }
        Object credentials = authentication.getCredentials();
        if (credentials instanceof Jwt jwt) {
            return jwt;
        }
        throw new UnauthorizedException("Unauthorized");
    }

    private boolean isEmailVerified(Jwt jwt) {
        Boolean verified = jwt.getClaimAsBoolean("email_verified");
        if (verified != null) {
            return verified;
        }
        String rawValue = jwt.getClaimAsString("email_verified");
        if (StringUtils.hasText(rawValue)) {
            return "true".equalsIgnoreCase(rawValue);
        }
        String tokenUse = jwt.getClaimAsString("token_use");
        return "access".equals(tokenUse);
    }

    private String resolveTokenEmail(Jwt jwt) {
        String email = safeNormalizeEmail(jwt.getClaimAsString("email"));
        if (StringUtils.hasText(email)) {
            return email;
        }

        String username = safeTrim(jwt.getClaimAsString("username"));
        if (!StringUtils.hasText(username)) {
            username = safeTrim(jwt.getClaimAsString("cognito:username"));
        }
        if (!StringUtils.hasText(username)) {
            username = safeTrim(jwt.getClaimAsString("preferred_username"));
        }

        return safeNormalizeEmail(username);
    }

    private String safeNormalizeEmail(String value) {
        try {
            return normalizeEmail(value);
        } catch (IllegalArgumentException ex) {
            return null;
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
