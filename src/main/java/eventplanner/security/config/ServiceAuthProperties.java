package eventplanner.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "service.auth")
public class ServiceAuthProperties {

    private String apiKey;
    private Boolean enabled;
    private Boolean requireHeader;
    private String allowServiceRolePaths;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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
