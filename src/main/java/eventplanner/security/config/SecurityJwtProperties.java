package eventplanner.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class SecurityJwtProperties {

    private Boolean autoProvision;

    public Boolean getAutoProvision() {
        return autoProvision;
    }

    public void setAutoProvision(Boolean autoProvision) {
        this.autoProvision = autoProvision;
    }
}
