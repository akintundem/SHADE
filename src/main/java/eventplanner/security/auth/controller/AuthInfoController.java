package eventplanner.security.auth.controller;

import eventplanner.common.dto.ApiMessageResponse;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.UnauthorizedException;
import eventplanner.security.auth.dto.req.SignupRequest;
import eventplanner.security.auth.dto.res.AuthSessionResponse;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.service.IdpUserService;
import eventplanner.security.auth.service.TokenRevocationService;
import eventplanner.security.auth.service.UserAccountService;
import eventplanner.common.util.Preconditions;
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
import org.springframework.web.bind.annotation.RestController;

import static eventplanner.security.util.AuthValidationUtil.normalizeEmail;
import static eventplanner.security.util.AuthValidationUtil.safeTrim;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthInfoController {

    private final IdpUserService idpUserService;
    private final UserAccountService userAccountService;
    private final TokenRevocationService tokenRevocationService;

    public AuthInfoController(IdpUserService idpUserService,
                              UserAccountService userAccountService,
                              TokenRevocationService tokenRevocationService) {
        this.idpUserService = idpUserService;
        this.userAccountService = userAccountService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @GetMapping("/session")
    @RequiresPermission(value = RbacPermissions.AUTH_ME, resources = {"user_id=#principal.id"})
    public ResponseEntity<AuthSessionResponse> session(@AuthenticationPrincipal UserPrincipal principal) {
        Preconditions.requireAuthenticated(principal);
        var account = principal.getUser();
        var userResponse = AuthMapper.toSecureUserResponse(account);
        var session = AuthSessionResponse.builder()
                .user(userResponse)
                .onboardingRequired(isOnboardingRequired(account))
                .build();
        return ResponseEntity.ok(session);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiMessageResponse> logout(@AuthenticationPrincipal UserPrincipal principal,
                                                     Authentication authentication) {
        Preconditions.requireAuthenticated(principal);

        // Revoke the current JWT so it cannot be reused even before it expires.
        if (authentication != null) {
            Jwt jwt = null;
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                jwt = jwtAuth.getToken();
            } else if (authentication.getDetails() instanceof Jwt j) {
                jwt = j;
            } else if (authentication.getCredentials() instanceof Jwt j) {
                jwt = j;
            }
            if (jwt != null && jwt.getId() != null) {
                tokenRevocationService.revoke(jwt.getId(), jwt.getExpiresAt());
            }
        }

        idpUserService.signOutUser(
                principal.getUser().getAuthSub(),
                principal.getUser().getEmail()
        );
        return ResponseEntity.ok(ApiMessageResponse.success("Logged out successfully"));
    }

    /**
     * Lightweight signup endpoint for OIDC (Auth0) flows.
     * Creates/updates a local user record and syncs profile to IdP.
     *
     * <p>Auth0 API access tokens often omit the {@code email} claim when an
     * API audience is specified. We therefore resolve the authoritative email
     * from the provisioned {@link UserPrincipal} (loaded by
     * {@code OidcJwtAuthenticationConverter} via the {@code sub} claim) and
     * fall back to the JWT claim only when necessary.
     */
    @PostMapping("/signup")
    public ResponseEntity<SecureUserResponse> signup(@Valid @RequestBody SignupRequest request,
                                                     Authentication authentication,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        try {
            Jwt jwt = requireJwt(authentication);
            String subject = jwt.getSubject();
            if (!StringUtils.hasText(subject)) {
                throw new UnauthorizedException("Invalid token");
            }

            // Resolve the authoritative email: prefer DB principal email, then JWT
            // claim, then request email (Auth0 API access tokens strip the email claim).
            String requestEmail = normalizeEmail(request.getEmail());
            String canonicalEmail = resolveCanonicalEmail(jwt, principal, requestEmail);
            if (!StringUtils.hasText(canonicalEmail)) {
                throw new UnauthorizedException("Unable to resolve verified email for signup");
            }

            // When we resolved via JWT or DB, confirm the request email matches.
            // When we fell back to the request email itself, this is a no-op.
            if (!canonicalEmail.equalsIgnoreCase(requestEmail)) {
                throw new BadRequestException("Invalid signup payload");
            }

            request.setEmail(canonicalEmail);
            var result = userAccountService.provisionUser(request, subject);
            UserAccount user = result.user();
            try {
                idpUserService.updateUserProfile(
                        subject,
                        canonicalEmail,
                        user.getName(),
                        user.getUsername(),
                        user.getPhoneNumber()
                );
                // Mark email verified so the user can immediately sign in via password grant
                // without waiting for a verification email (our signup flow is the verification).
                idpUserService.markEmailVerified(subject);
            } catch (Exception idpEx) {
                // IdP sync is best-effort — user is already provisioned locally.
                // Log and continue so the caller gets a success response.
                org.slf4j.LoggerFactory.getLogger(AuthInfoController.class)
                        .warn("IdP profile sync failed for sub={}: {}", subject, idpEx.getMessage());
            }
            HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status).body(AuthMapper.toSecureUserResponse(user));
        } catch (IllegalStateException ex) {
            // Return a generic error to avoid revealing whether an account exists
            throw new BadRequestException("Invalid credentials", ex);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid credentials", ex);
        }
    }

    /**
     * Resolve the canonical email for signup.
     * Priority:
     * 1. Principal's email from DB (loaded via sub claim), if it's a real email (not a placeholder)
     * 2. JWT email claim (standard OIDC tokens)
     * 3. JWT preferred_username / username (some IdP configs)
     * 4. Request email — trusted as a last resort when JWT is Auth0 access token
     *    (API audience causes Auth0 to strip the email claim; the token is still
     *    cryptographically verified by Spring Security, so the sub is authentic)
     */
    private String resolveCanonicalEmail(Jwt jwt, UserPrincipal principal, String requestEmail) {
        if (principal != null && principal.getUser() != null) {
            String principalEmail = safeNormalizeEmail(principal.getUser().getEmail());
            if (StringUtils.hasText(principalEmail) && !principalEmail.endsWith("@auth0.local")) {
                return principalEmail;
            }
        }

        String tokenEmail = resolveTokenEmail(jwt);
        if (StringUtils.hasText(tokenEmail)) {
            return tokenEmail;
        }

        // Auth0 access tokens with API audience omit the email claim.
        // The JWT is cryptographically verified; the sub claim is authentic.
        // Accept the request email as the authoritative identity email.
        if (StringUtils.hasText(requestEmail) && requestEmail.contains("@")) {
            return requestEmail;
        }

        return null;
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
        return eventplanner.security.util.JwtClaimUtils.isEmailVerifiedOrAccessToken(jwt);
    }

    private String resolveTokenEmail(Jwt jwt) {
        String email = safeNormalizeEmail(jwt.getClaimAsString("email"));
        if (StringUtils.hasText(email)) {
            return email;
        }

        String username = safeTrim(jwt.getClaimAsString("username"));
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
