package eventplanner.common.storage.s3.config;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import jakarta.annotation.PostConstruct;
import java.net.URI;

/**
 * AWS S3 Configuration.
 * Spring configuration for S3 client and presigner beans.
 */
@Configuration
@EnableConfigurationProperties(AwsS3Properties.class)
@RequiredArgsConstructor
public class AwsS3Configuration {

    private final AwsS3Properties properties;

    @PostConstruct
    public void validateConfiguration() {
        if (!properties.hasAwsBasicsConfigured()) {
            throw new IllegalStateException(
                "AWS S3 configuration is required but missing. " +
                "Set aws.s3.access-key-id, aws.s3.secret-access-key, and aws.s3.region"
            );
        }
        if (properties.getBuckets().isEmpty() && !StringUtils.hasText(properties.getDefaultBucket())) {
            throw new IllegalStateException(
                "At least one S3 bucket must be configured. " +
                "Set aws.s3.buckets.* (e.g., aws.s3.buckets.user: bucket-name) or aws.s3.default-bucket"
            );
        }
    }

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey())
                ));

        if (Boolean.TRUE.equals(properties.getPathStyleAccessEnabled())) {
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }

        if (StringUtils.hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey())
                ));

        if (Boolean.TRUE.equals(properties.getPathStyleAccessEnabled())) {
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }

        if (StringUtils.hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }
}
