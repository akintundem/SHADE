package eventplanner.security.auth.jwt;

import eventplanner.security.auth.enums.UserStatus;
import eventplanner.security.auth.enums.UserType;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSettings;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.config.SecurityJwtProperties;
import eventplanner.security.util.AuthValidationUtil;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Converts a validated Cognito JWT into a UserPrincipal-backed Authentication,
 * provisioning a user account on first sight if enabled.
 */
@Component
public class CognitoJwtAuthenticationConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final UserAccountRepository userAccountRepository;
    private final boolean autoProvision;

    public CognitoJwtAuthenticationConverter(UserAccountRepository userAccountRepository,
                                             SecurityJwtProperties securityJwtProperties) {
        this.userAccountRepository = userAccountRepository;
        this.autoProvision = requireConfigured(securityJwtProperties.getAutoProvision(), "security.jwt.auto-provision");
    }

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        String subject = safeTrim(jwt.getSubject());
        if (!StringUtils.hasText(subject)) {
            throw invalidToken("Missing subject in token");
        }

        String email = normalizeEmailClaim(jwt.getClaimAsString("email"));
        boolean emailVerified = isEmailVerified(jwt);
        String name = resolveDisplayName(jwt);

        var user = resolveOrProvisionUser(subject, email, emailVerified, name);
        if (user.getUserType() == UserType.ADMIN) {
            throw invalidToken("Admin identities must use the admin identity provider");
        }
        UserPrincipal principal = new UserPrincipal(user, List.of(), null);

        List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());
        authorities.addAll(mapGroupsToAuthorities(jwt));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                new BearerTokenAuthenticationToken(jwt.getTokenValue()),
                authorities
        );
        authentication.setDetails(jwt);
        return authentication;
    }

    private boolean isEmailVerified(Jwt jwt) {
        return eventplanner.security.util.JwtClaimUtils.isEmailVerified(jwt);
    }

    private OAuth2AuthenticationException invalidToken(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_token", message, null));
    }

    private List<GrantedAuthority> mapGroupsToAuthorities(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String group : groups) {
            if (!StringUtils.hasText(group)) {
                continue;
            }
            String normalized = group.trim().toUpperCase(Locale.ROOT);
            if ("ADMIN".equals(normalized) || "SUPER_ADMIN".equals(normalized)) {
                throw invalidToken("Admin identities must use the admin identity provider");
            }
            authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
        }
        return authorities;
    }

    private UserAccount resolveOrProvisionUser(String subject, String email, boolean emailVerified, String name) {
        return userAccountRepository.findByCognitoSub(subject)
                .map(existing -> updateUserFromClaims(existing, subject, email, emailVerified, name))
                .orElseGet(() -> linkOrProvisionUser(subject, email, emailVerified, name));
    }

    private UserAccount linkOrProvisionUser(String subject, String email, boolean emailVerified, String name) {
        // Only link by email when the token proves the email is verified.
        // Without this check an attacker with an unverified email could hijack
        // an existing account.
        if (StringUtils.hasText(email) && emailVerified) {
            var existingByEmail = userAccountRepository.findByEmailIgnoreCase(email);
            if (existingByEmail.isPresent()) {
                UserAccount user = existingByEmail.get();
                String existingSub = safeTrim(user.getCognitoSub());
                if (StringUtils.hasText(existingSub) && !existingSub.equals(subject)) {
                    throw invalidToken("Token subject does not match existing account");
                }
                user.setCognitoSub(subject);
                return updateUserFromClaims(user, subject, email, emailVerified, name);
            }
        }

        if (!autoProvision) {
            if (isSignupRequest()) {
                return createSignupPlaceholder(subject, email, name);
            }
            throw invalidToken("User not found and auto-provision disabled");
        }

        return provisionNewUser(subject, email, name);
    }

    private static boolean requireConfigured(Boolean value, String propertyName) {
        return eventplanner.common.util.Preconditions.requireConfigured(value, propertyName);
    }

    private UserAccount updateUserFromClaims(UserAccount user, String subject, String email, boolean emailVerified, String name) {
        boolean updated = false;

        // Only update the email when the JWT proves the new address is verified.
        if (emailVerified && StringUtils.hasText(email) && !email.equalsIgnoreCase(user.getEmail())) {
            ensureUniqueEmail(email, user.getId());
            user.setEmail(email);
            updated = true;
        }

        if (StringUtils.hasText(name) && !name.equals(user.getName())) {
            user.setName(name);
            updated = true;
        }

        if (StringUtils.hasText(subject) && !subject.equals(user.getCognitoSub())) {
            user.setCognitoSub(subject);
            updated = true;
        }

        if (user.getSettings() == null) {
            user.setSettings(UserSettings.createDefault(user));
            updated = true;
        }

        return updated ? userAccountRepository.save(user) : user;
    }

    private UserAccount provisionNewUser(String subject, String email, String name) {
        String normalizedEmail = normalizeEmailOrFallback(subject, email);
        String resolvedName = StringUtils.hasText(name) ? name.trim() : normalizedEmail;
        UserAccount user = UserAccount.builder()
                .email(normalizedEmail)
                .cognitoSub(subject)
                .name(resolvedName)
                .acceptTerms(false)
                .acceptPrivacy(false)
                .marketingOptIn(false)
                .userType(UserType.INDIVIDUAL)
                .status(UserStatus.ACTIVE)
                .profileCompleted(false)
                .build();
        user.setSettings(UserSettings.createDefault(user));
        return userAccountRepository.save(user);
    }

    private UserAccount createSignupPlaceholder(String subject, String email, String name) {
        String normalizedEmail = normalizeEmailOrFallback(subject, email);
        String resolvedName = StringUtils.hasText(name) ? name.trim() : normalizedEmail;
        UserAccount user = UserAccount.builder()
                .email(normalizedEmail)
                .cognitoSub(subject)
                .name(resolvedName)
                .acceptTerms(false)
                .acceptPrivacy(false)
                .marketingOptIn(false)
                .userType(UserType.INDIVIDUAL)
                .status(UserStatus.ACTIVE)
                .profileCompleted(false)
                .build();
        user.setSettings(UserSettings.createDefault(user));
        return user;
    }

    private String resolveDisplayName(Jwt jwt) {
        String name = safeTrim(jwt.getClaimAsString("name"));
        if (StringUtils.hasText(name)) {
            return name;
        }

        String givenName = safeTrim(jwt.getClaimAsString("given_name"));
        String familyName = safeTrim(jwt.getClaimAsString("family_name"));
        if (StringUtils.hasText(givenName) || StringUtils.hasText(familyName)) {
            String composite = (StringUtils.hasText(givenName) ? givenName + " " : "")
                    + (StringUtils.hasText(familyName) ? familyName : "");
            return composite.trim();
        }

        String preferredUsername = safeTrim(jwt.getClaimAsString("cognito:username"));
        if (!StringUtils.hasText(preferredUsername)) {
            preferredUsername = safeTrim(jwt.getClaimAsString("preferred_username"));
        }
        if (StringUtils.hasText(preferredUsername)) {
            return preferredUsername;
        }

        String email = normalizeEmailClaim(jwt.getClaimAsString("email"));
        if (StringUtils.hasText(email)) {
            return email.split("@")[0];
        }
        return jwt.getSubject();
    }

    private String normalizeEmailOrFallback(String subject, String email) {
        if (StringUtils.hasText(email)) {
            return email.trim().toLowerCase(Locale.ROOT);
        }
        String fallback = StringUtils.hasText(subject) ? subject : UUID.randomUUID().toString();
        return fallback + "@cognito.local";
    }

    private boolean isSignupRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return false;
        }
        var request = servletAttributes.getRequest();
        if (request == null) {
            return false;
        }
        String path = request.getRequestURI();
        return "/api/v1/auth/signup".equals(path) || "/api/v1/auth/signup/".equals(path);
    }

    private String normalizeEmailClaim(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String safeTrim(String value) {
        return AuthValidationUtil.safeTrim(value);
    }

    private void ensureUniqueEmail(String email, UUID currentUserId) {
        userAccountRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw invalidToken("Email already linked to another account");
                });
    }
}
