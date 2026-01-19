package eventplanner.common.storage.s3.config;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * AWS S3 configuration properties.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "aws.s3")
public class AwsS3Properties {

    private String accessKeyId;
    private String secretAccessKey;
    private String region;

    /**
     * Map of bucket aliases to bucket names.
     * Add new buckets by adding entries: aws.s3.buckets.your-alias: bucket-name
     */
    private Map<String, String> buckets = new HashMap<>();

    /**
     * Default bucket to use when no bucket is specified.
     * Optional - if not set, bucket must be explicitly provided.
     */
    private String defaultBucket;

    private String endpoint;
    private Boolean pathStyleAccessEnabled = false;

    public boolean hasAwsBasicsConfigured() {
        return StringUtils.hasText(accessKeyId)
                && StringUtils.hasText(secretAccessKey)
                && StringUtils.hasText(region);
    }
}
