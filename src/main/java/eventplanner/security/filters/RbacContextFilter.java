package eventplanner.security.filters;

import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.features.event.entity.EventRole;
import eventplanner.features.event.repository.EventRoleRepository;
import eventplanner.security.authorization.rbac.RbacRequestContext;
import eventplanner.security.authorization.rbac.RbacRequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enriches the SecurityContext with event roles and prepares the RBAC request context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RbacContextFilter extends OncePerRequestFilter {

    private final EventRoleRepository eventRoleRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
                RbacRequestContext context = buildContext(principal);
                RbacRequestContextHolder.set(context);
                refreshAuthentication(authentication, principal, context);
            }
            filterChain.doFilter(request, response);
        } finally {
            RbacRequestContextHolder.clear();
        }
    }

    private RbacRequestContext buildContext(UserPrincipal principal) {
        UUID userId = principal.getId();
        if (userId == null) {
            return RbacRequestContext.builder()
                    .systemRoles(Set.of())
                    .eventRoles(Map.of())
                    .build();
        }

        Map<UUID, Set<String>> eventRoles = eventRoleRepository.findByUserId(userId).stream()
                .filter(role -> Boolean.TRUE.equals(role.getIsActive()))
                .collect(Collectors.groupingBy(
                        EventRole::getEventId,
                        Collectors.mapping(role -> role.getRoleName().name().toUpperCase(), Collectors.toSet())
                ));

        Set<String> systemRoles = new LinkedHashSet<>();
        systemRoles.add("USER");
        if (principal.isSystemAdmin()) {
            systemRoles.add("SUPER_ADMIN");
        }

        return RbacRequestContext.builder()
                .userId(userId)
                .systemRoles(systemRoles)
                .eventRoles(eventRoles)
                .build();
    }

    private void refreshAuthentication(Authentication currentAuth, UserPrincipal originalPrincipal, RbacRequestContext context) {
        List<String> eventRoles = context.getEventRoles().values().stream()
                .flatMap(Set::stream)
                .distinct()
                .toList();

        UserPrincipal enriched = new UserPrincipal(
                originalPrincipal.getUser(),
                eventRoles,
                originalPrincipal.getDeviceId()
        );

        UsernamePasswordAuthenticationToken updatedAuth = new UsernamePasswordAuthenticationToken(
                enriched,
                currentAuth.getCredentials(),
                enriched.getAuthorities()
        );
        updatedAuth.setDetails(currentAuth.getDetails());
        SecurityContextHolder.getContext().setAuthentication(updatedAuth);
    }
}
