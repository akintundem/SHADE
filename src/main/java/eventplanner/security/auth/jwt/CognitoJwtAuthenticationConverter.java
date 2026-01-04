package eventplanner.security.auth.jwt;

import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSettings;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.core.convert.converter.Converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Converts a validated Cognito JWT into a UserPrincipal-backed Authentication,
 * provisioning a user account on first sight if enabled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CognitoJwtAuthenticationConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final UserAccountRepository userAccountRepository;

    @Value("${security.jwt.auto-provision:true}")
    private boolean autoProvision;

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        String subject = jwt.getSubject();
        if (!StringUtils.hasText(subject)) {
            throw invalidToken("Missing subject in token");
        }

        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        UserAccount user = userAccountRepository.findByCognitoSub(subject)
                .orElseGet(() -> provisionIfAllowed(subject, email, name));

        UserPrincipal principal = new UserPrincipal(user);
        List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());

        // Map Cognito groups to Spring authorities (ROLE_<GROUP_NAME>)
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        if (groups != null) {
            groups.stream()
                    .filter(StringUtils::hasText)
                    .map(group -> "ROLE_" + group.toUpperCase(Locale.ROOT))
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        return new UsernamePasswordAuthenticationToken(principal, new BearerTokenAuthenticationToken(jwt.getTokenValue()), authorities);
    }

    private UserAccount provisionIfAllowed(String subject, String email, String name) {
        if (!autoProvision) {
            throw invalidToken("User not found and auto-provision disabled");
        }

        String normalizedEmail = normalizeEmailOrFallback(subject, email);
        UserAccount user = UserAccount.builder()
                .email(normalizedEmail)
                .cognitoSub(subject)
                .name(StringUtils.hasText(name) ? name : normalizedEmail)
                .acceptTerms(false)
                .acceptPrivacy(false)
                .marketingOptIn(false)
                .userType(eventplanner.common.domain.enums.UserType.INDIVIDUAL)
                .status(eventplanner.common.domain.enums.UserStatus.ACTIVE)
                .profileCompleted(false)
                .build();
        user.setSettings(UserSettings.createDefault(user));
        return userAccountRepository.save(user);
    }

    private String normalizeEmailOrFallback(String subject, String email) {
        if (StringUtils.hasText(email)) {
            return email.trim().toLowerCase(Locale.ROOT);
        }
        String fallback = StringUtils.hasText(subject) ? subject : UUID.randomUUID().toString();
        return fallback + "@cognito.local";
    }

    private OAuth2AuthenticationException invalidToken(String message) {
        log.warn("JWT authentication failed: {}", message);
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_token", message, null));
    }
}
