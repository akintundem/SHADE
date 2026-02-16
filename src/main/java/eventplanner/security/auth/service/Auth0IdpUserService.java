package eventplanner.security.auth.service;

import eventplanner.security.config.Auth0Properties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Auth0 Management API implementation of IdpUserService.
 * No-ops when auth0.management is not configured.
 */
@Service
public class Auth0IdpUserService implements IdpUserService {

    private final Auth0Properties auth0;
    private final RestTemplate restTemplate = new RestTemplate();

    public Auth0IdpUserService(Auth0Properties auth0) {
        this.auth0 = auth0;
    }

    private boolean isConfigured() {
        return auth0 != null
                && StringUtils.hasText(auth0.getDomain())
                && StringUtils.hasText(auth0.getClientId())
                && StringUtils.hasText(auth0.getClientSecret());
    }

    private String baseUrl() {
        String domain = auth0.getDomain().trim().replaceAll("^https?://", "").replaceAll("/$", "");
        return "https://" + domain;
    }

    private String getManagementToken() {
        String url = baseUrl() + "/oauth/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", auth0.getClientId());
        body.add("client_secret", auth0.getClientSecret());
        body.add("audience", baseUrl() + "/api/v2/");
        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        if (response.getBody() != null && response.getBody().get("access_token") != null) {
            return (String) response.getBody().get("access_token");
        }
        throw new IllegalStateException("Auth0 Management API token request failed");
    }

    @Override
    public void deleteUser(String authSub, String emailFallback) {
        if (!isConfigured() || !StringUtils.hasText(authSub)) {
            return;
        }
        try {
            String token = getManagementToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            String url = baseUrl() + "/api/v2/users/" + authSub;
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception ignored) {
            // no-op on failure
        }
    }

    @Override
    public void signOutUser(String authSub, String emailFallback) {
        // Auth0: revoke refresh tokens would require storing them or using Management API "delete user sessions" if available.
        // For logout we typically just clear client-side tokens; server-side revoke is optional.
        // No-op here; client clears tokens.
    }

    @Override
    public void updateUserProfile(String authSub,
                                  String emailFallback,
                                  String name,
                                  String preferredUsername,
                                  String phoneNumber) {
        if (!isConfigured() || !StringUtils.hasText(authSub)) {
            return;
        }
        try {
            String token = getManagementToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            Map<String, Object> body = new java.util.HashMap<>();
            if (StringUtils.hasText(name)) {
                body.put("name", name.trim());
            }
            if (StringUtils.hasText(preferredUsername)) {
                body.put("user_metadata", Map.of("username", preferredUsername.trim()));
            }
            if (StringUtils.hasText(phoneNumber)) {
                body.put("phone_number", phoneNumber.trim());
            }
            if (body.isEmpty()) {
                return;
            }
            String url = baseUrl() + "/api/v2/users/" + authSub;
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body, headers), Map.class);
        } catch (Exception ignored) {
            // no-op on failure
        }
    }
}
