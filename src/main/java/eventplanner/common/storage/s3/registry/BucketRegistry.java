package eventplanner.common.storage.s3.registry;

import eventplanner.common.storage.s3.config.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Registry for resolving S3 bucket aliases to actual bucket names.
 * Buckets are configured via AwsS3Properties.
 */
@Component
@RequiredArgsConstructor
public class BucketRegistry {

    private final AwsS3Properties properties;

    /**
     * Resolves a bucket alias or name to the actual bucket name.
     * 
     * @param aliasOrName Bucket alias (e.g., "event", "user") or actual bucket name.
     *                    If null or empty, returns the default bucket.
     * @return The resolved bucket name
     * @throws IllegalStateException if bucket cannot be resolved
     */
    public String resolve(String aliasOrName) {
        // If no alias/name provided, use default bucket
        if (!StringUtils.hasText(aliasOrName)) {
            String defaultBucket = properties.getDefaultBucket();
            if (!StringUtils.hasText(defaultBucket)) {
                throw new IllegalStateException(
                    "No bucket specified and no default bucket configured. " +
                    "Set aws.s3.default-bucket or provide a bucket alias/name."
                );
            }
            return defaultBucket;
        }

        // Check if it's an alias in the buckets map
        Map<String, String> buckets = properties.getBuckets();
        if (buckets != null && buckets.containsKey(aliasOrName)) {
            return buckets.get(aliasOrName);
        }

        // If not found as alias, assume it's already a bucket name
        // This allows using actual bucket names directly
        return aliasOrName;
    }

    /**
     * Checks if a bucket alias exists in the registry.
     * 
     * @param alias The bucket alias to check
     * @return true if the alias exists, false otherwise
     */
    public boolean hasAlias(String alias) {
        if (!StringUtils.hasText(alias)) {
            return false;
        }
        Map<String, String> buckets = properties.getBuckets();
        return buckets != null && buckets.containsKey(alias);
    }
}
