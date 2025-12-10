package eventplanner.common.storage.s3.config;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "aws.s3")
public class AwsS3Properties {

    private String accessKeyId;
    private String secretAccessKey;
    private String region;

    /**
     * Map of named buckets. For now we expect "user" and "event" keys.
     */
    private Map<String, String> buckets = new HashMap<>();

    private String endpoint;
    private Boolean pathStyleAccessEnabled = false;

    public boolean hasAwsBasicsConfigured() {
        return StringUtils.hasText(accessKeyId)
                && StringUtils.hasText(secretAccessKey)
                && StringUtils.hasText(region);
    }

    public boolean isConfigured() {
        return hasAwsBasicsConfigured()
                && StringUtils.hasText(getUserBucket())
                && StringUtils.hasText(getEventBucket());
    }

    public String getUserBucket() {
        return buckets.get("user");
    }

    public String getEventBucket() {
        return buckets.get("event");
    }

    public String resolveBucket(String aliasOrName) {
        if (StringUtils.hasText(aliasOrName)) {
            String mapped = buckets.get(aliasOrName);
            return StringUtils.hasText(mapped) ? mapped : aliasOrName;
        }
        // Default to user bucket when an alias is not provided
        return getUserBucket();
    }
}
