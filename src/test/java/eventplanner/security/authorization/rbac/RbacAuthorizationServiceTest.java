package eventplanner.security.authorization.rbac;

import eventplanner.common.domain.enums.UserType;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.service.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RbacAuthorizationServiceTest {

    private RbacPolicyStore policyStore;
    private AuthorizationService authorizationService;
    private RbacAuthorizationService authorization;

    @BeforeEach
    void setUp() {
        policyStore = new RbacPolicyStore(new DefaultResourceLoader());
        ReflectionTestUtils.setField(policyStore, "policyLocation", "classpath:rbac/RBAC_policy.yml");
        policyStore.loadPolicy();

        authorizationService = Mockito.mock(AuthorizationService.class);
        authorization = new RbacAuthorizationService(policyStore, authorizationService);
    }

    @AfterEach
    void teardown() {
        RbacRequestContextHolder.clear();
    }

    @Test
    void allowsUserUpdatingOwnProfile() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = principal(userId);
        Map<String, Object> resources = Map.of("user_id", userId);

        assertTrue(authorization.isAuthorized(principal, RbacPermissions.USER_UPDATE, resources));
    }

    @Test
    void blocksUserUpdatingDifferentProfile() {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserPrincipal principal = principal(userId);
        Map<String, Object> resources = Map.of("user_id", targetId);

        assertFalse(authorization.isAuthorized(principal, RbacPermissions.USER_UPDATE, resources));
    }

    @Test
    void resolvesEventRoleFromContext() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UserPrincipal principal = principal(userId);
        RbacRequestContextHolder.set(RbacRequestContext.builder()
                .userId(userId)
                .systemRoles(Set.of("USER"))
                .eventRoles(Map.of(eventId, Set.of("ORGANIZER")))
                .build());

        Map<String, Object> resources = Map.of("event_id", eventId);
        assertTrue(authorization.isAuthorized(principal, RbacPermissions.EVENT_PUBLISH, resources));
    }

    @Test
    void invalidResourceUuidRaisesBadRequest() {
        UserPrincipal principal = principal(UUID.randomUUID());
        Map<String, Object> resources = Map.of("event_id", "not-a-uuid");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authorization.isAuthorized(principal, RbacPermissions.EVENT_PUBLISH, resources));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    private UserPrincipal principal(UUID userId) {
        UserAccount account = UserAccount.builder()
                .email("user@example.com")
                .name("Test User")
                .userType(UserType.INDIVIDUAL)
                .passwordHash("secret")
                .build();
        account.setId(userId);
        return new UserPrincipal(account);
    }
}
