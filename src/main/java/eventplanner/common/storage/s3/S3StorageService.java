package eventplanner.common.storage.s3;

import eventplanner.common.storage.s3.config.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3StorageService {

    private static final Duration DEFAULT_PRESIGN_DURATION = Duration.ofMinutes(15);

    private final AwsS3Properties properties;
    private final Optional<S3Client> s3Client;
    private final Optional<S3Presigner> s3Presigner;

    public String uploadObject(String key, InputStream content, long contentLength, String contentType) {
        return uploadObject(null, key, content, contentLength, contentType);
    }

    public String uploadObject(String bucketAliasOrName, String key, InputStream content, long contentLength, String contentType) {
        S3Client client = requireClient();
        String bucket = resolveBucket(bucketAliasOrName);
        PutObjectRequest.Builder request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);

        if (StringUtils.hasText(contentType)) {
            request.contentType(contentType);
        }

        client.putObject(request.build(), RequestBody.fromInputStream(content, contentLength));
        log.debug("Uploaded object {} to bucket {}", key, bucket);
        return key;
    }

    public void deleteObject(String key) {
        deleteObject(null, key);
    }

    public void deleteObject(String bucketAliasOrName, String key) {
        S3Client client = requireClient();
        String bucket = resolveBucket(bucketAliasOrName);
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
        log.debug("Deleted object {} from bucket {}", key, bucket);
    }

    public URL generatePresignedGetUrl(String key, Duration expiresIn) {
        return generatePresignedGetUrl(null, key, expiresIn);
    }

    public URL generatePresignedGetUrl(String bucketAliasOrName, String key, Duration expiresIn) {
        S3Presigner presigner = requirePresigner();
        String bucket = resolveBucket(bucketAliasOrName);
        Duration duration = expiresIn != null ? expiresIn : DEFAULT_PRESIGN_DURATION;

        PresignedGetObjectRequest presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build());

        return presigned.url();
    }

    public URL generatePresignedPutUrl(String key, Duration expiresIn, String contentType) {
        return generatePresignedPutUrl(null, key, expiresIn, contentType);
    }

    public URL generatePresignedPutUrl(String bucketAliasOrName, String key, Duration expiresIn, String contentType) {
        S3Presigner presigner = requirePresigner();
        String bucket = resolveBucket(bucketAliasOrName);
        Duration duration = expiresIn != null ? expiresIn : DEFAULT_PRESIGN_DURATION;

        PutObjectRequest.Builder putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);

        if (StringUtils.hasText(contentType)) {
            putRequest.contentType(contentType);
        }

        PresignedPutObjectRequest presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(duration)
                .putObjectRequest(putRequest.build())
                .build());

        return presigned.url();
    }

    public boolean isConfigured() {
        return properties.hasAwsBasicsConfigured() && s3Client.isPresent() && s3Presigner.isPresent();
    }

    private S3Client requireClient() {
        return s3Client.orElseThrow(() -> new IllegalStateException(
                "AWS S3 is not configured. Set aws.s3.* credentials and bucket."));
    }

    private S3Presigner requirePresigner() {
        return s3Presigner.orElseThrow(() -> new IllegalStateException(
                "AWS S3 is not configured. Set aws.s3.* credentials and bucket."));
    }

    private String resolveBucket(String bucketAliasOrName) {
        String bucket = properties.resolveBucket(bucketAliasOrName);
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("No S3 bucket configured for alias '" + bucketAliasOrName + "'. " +
                    "Ensure aws.s3.buckets.user and aws.s3.buckets.event are set.");
        }
        return bucket;
    }
}
