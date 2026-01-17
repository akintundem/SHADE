package eventplanner.common.storage.s3.services;

import eventplanner.common.storage.s3.registry.BucketRegistry;
import lombok.RequiredArgsConstructor;
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
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Centralized S3 storage service for uploading to different buckets.
 * S3 is mandatory for application startup.
 * 
 * Uses BucketRegistry for flexible bucket management - add new buckets via configuration only.
 */
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private static final Duration DEFAULT_PRESIGN_DURATION = Duration.ofMinutes(15);
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("(\\.\\./|\\.\\.\\\\)");
    private static final int MAX_KEY_LENGTH = 1024;

    private final BucketRegistry bucketRegistry;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public String uploadObject(String key, InputStream content, long contentLength, String contentType) {
        return uploadObject(null, key, content, contentLength, contentType);
    }

    public String uploadObject(String bucketAliasOrName, String key, InputStream content, long contentLength, String contentType) {
        validateObjectKey(key);
        String bucket = bucketRegistry.resolve(bucketAliasOrName);
        PutObjectRequest.Builder request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);

        if (StringUtils.hasText(contentType)) {
            request.contentType(contentType);
        }

        s3Client.putObject(request.build(), RequestBody.fromInputStream(content, contentLength));
        return key;
    }

    public void deleteObject(String key) {
        deleteObject(null, key);
    }

    public void deleteObject(String bucketAliasOrName, String key) {
        validateObjectKey(key);
        String bucket = bucketRegistry.resolve(bucketAliasOrName);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    public URL generatePresignedGetUrl(String key, Duration expiresIn) {
        return generatePresignedGetUrl(null, key, expiresIn);
    }

    public URL generatePresignedGetUrl(String bucketAliasOrName, String key, Duration expiresIn) {
        validateObjectKey(key);
        String bucket = bucketRegistry.resolve(bucketAliasOrName);
        Duration duration = expiresIn != null ? expiresIn : DEFAULT_PRESIGN_DURATION;

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
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
        validateObjectKey(key);
        String bucket = bucketRegistry.resolve(bucketAliasOrName);
        Duration duration = expiresIn != null ? expiresIn : DEFAULT_PRESIGN_DURATION;

        PutObjectRequest.Builder putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);

        if (StringUtils.hasText(contentType)) {
            putRequest.contentType(contentType);
        }

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(duration)
                .putObjectRequest(putRequest.build())
                .build());

        return presigned.url();
    }

    /**
     * Strips the query string and fragment from a URL.
     * Useful when storing/displaying "resource URLs" that must not contain presign credentials.
     */
    public String stripQuery(URL url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = url.toURI();
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null).toString();
        } catch (Exception ex) {
            String asString = url.toString();
            int idx = asString.indexOf('?');
            return idx > 0 ? asString.substring(0, idx) : asString;
        }
    }

    /**
     * Validates object key to prevent path traversal attacks and ensure it meets S3 requirements.
     */
    private void validateObjectKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Object key cannot be null or empty");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Object key exceeds maximum length of " + MAX_KEY_LENGTH);
        }
        if (PATH_TRAVERSAL_PATTERN.matcher(key).find()) {
            throw new IllegalArgumentException("Object key contains path traversal sequences (../)");
        }
        // S3 keys cannot start with / or contain // (except at the beginning)
        if (key.startsWith("/") || key.contains("//")) {
            throw new IllegalArgumentException("Object key cannot start with '/' or contain '//'");
        }
    }

}


