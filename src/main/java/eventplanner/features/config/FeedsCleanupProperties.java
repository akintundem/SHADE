package eventplanner.features.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "feeds.cleanup")
public class FeedsCleanupProperties {

    private Integer maxAgeMinutes;
    private String cron;

    public Integer getMaxAgeMinutes() {
        return maxAgeMinutes;
    }

    public void setMaxAgeMinutes(Integer maxAgeMinutes) {
        this.maxAgeMinutes = maxAgeMinutes;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }
}
