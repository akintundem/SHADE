package eventplanner.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Auth0 Management API configuration for user lifecycle (update profile, delete, revoke).
 * Optional: if not set, IdP operations are no-ops.
 */
@ConfigurationProperties(prefix = "auth0.management")
public class Auth0Properties {

    private String domain;
    private String clientId;
    private String clientSecret;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
