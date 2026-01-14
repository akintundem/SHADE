package eventplanner.security.filters;

import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSettings;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * Trusts gateway-provided identity headers (after gateway verifies Cognito) and
 * materializes a UserPrincipal for downstream RBAC/authorization.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayIdentityFilter extends OncePerRequestFilter {

    private final UserAccountRepository userAccountRepository;

    @Value("${gateway.auth.user-id-header:X-User-Id}")
    private String userIdHeader;

    @Value("${gateway.auth.auto-provision:true}")
    private boolean autoProvision;

    @Value("${gateway.auth.require-user-header:false}")
    private boolean requireUserHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip if already authenticated (e.g., service API key)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String externalId = headerValue(request, userIdHeader);
        String email = null;
        String name = null;

        if (!StringUtils.hasText(externalId)) {
            if (requireUserHeader) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing user identity");
                return;
            } else {
                filterChain.doFilter(request, response);
                return;
            }
        }

        try {
            UserAccount user = resolveOrProvisionUser(externalId, email, name);
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    new UserPrincipal(user),
                    null,
                    new UserPrincipal(user).getAuthorities()
                );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (IllegalStateException ex) {
            log.warn("Gateway identity resolution failed: {}", ex.getMessage());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private UserAccount resolveOrProvisionUser(String externalId, String email, String name) {
        if (StringUtils.hasText(externalId)) {
            return userAccountRepository.findByCognitoSub(externalId)
                    .orElseGet(() -> provisionIfAllowed(externalId, email, name));
        }

        // Fallback to email-only lookup
        return userAccountRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> provisionIfAllowed(null, email, name));
    }

    private UserAccount provisionIfAllowed(String externalId, String email, String name) {
        if (!autoProvision) {
            throw new IllegalStateException("Auto-provisioning disabled and user not found");
        }

        String normalizedEmail = normalizeEmailOrFallback(externalId, email);
        UserAccount user = UserAccount.builder()
                .email(normalizedEmail)
                .cognitoSub(externalId)
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

    private String headerValue(HttpServletRequest request, String headerName) {
        return StringUtils.hasText(headerName) ? request.getHeader(headerName) : null;
    }

    private String normalizeEmailOrFallback(String externalId, String email) {
        if (StringUtils.hasText(email)) {
            return email.trim().toLowerCase(Locale.ROOT);
        }
        String fallback = StringUtils.hasText(externalId) ? externalId : UUID.randomUUID().toString();
        return fallback + "@gateway.local";
    }
}
