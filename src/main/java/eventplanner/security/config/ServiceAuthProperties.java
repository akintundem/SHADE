package eventplanner.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "service.auth")
public class ServiceAuthProperties {

    private String apiKey;
    /** Optional secondary key to allow zero-downtime API key rotation.
     *  During rotation: set api-key to the new key, set api-key-secondary to the old key.
     *  Once all callers are updated, clear api-key-secondary. */
    private String apiKeySecondary;
    private Boolean enabled;
    private Boolean requireHeader;
    private String allowServiceRolePaths;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKeySecondary() {
        return apiKeySecondary;
    }

    public void setApiKeySecondary(String apiKeySecondary) {
        this.apiKeySecondary = apiKeySecondary;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getRequireHeader() {
        return requireHeader;
    }

    public void setRequireHeader(Boolean requireHeader) {
        this.requireHeader = requireHeader;
    }

    public String getAllowServiceRolePaths() {
        return allowServiceRolePaths;
    }

    public void setAllowServiceRolePaths(String allowServiceRolePaths) {
        this.allowServiceRolePaths = allowServiceRolePaths;
    }
}
