package eventplanner.common.storage.s3.services;

import eventplanner.common.storage.s3.registry.BucketAlias;
import eventplanner.common.storage.s3.registry.BucketRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
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
        return uploadObject((String) null, key, content, contentLength, contentType);
    }

    public String uploadObject(BucketAlias bucketAlias, String key, InputStream content, long contentLength, String contentType) {
        String alias = bucketAlias != null ? bucketAlias.getAlias() : null;
        return uploadObject(alias, key, content, contentLength, contentType);
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
        deleteObject((String) null, key);
    }

    public void deleteObject(BucketAlias bucketAlias, String key) {
        String alias = bucketAlias != null ? bucketAlias.getAlias() : null;
        deleteObject(alias, key);
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
        return generatePresignedGetUrl((String) null, key, expiresIn);
    }

    public URL generatePresignedGetUrl(BucketAlias bucketAlias, String key, Duration expiresIn) {
        String alias = bucketAlias != null ? bucketAlias.getAlias() : null;
        return generatePresignedGetUrl(alias, key, expiresIn);
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
        return generatePresignedPutUrl((String) null, key, expiresIn, contentType);
    }

    public URL generatePresignedPutUrl(BucketAlias bucketAlias, String key, Duration expiresIn, String contentType) {
        String alias = bucketAlias != null ? bucketAlias.getAlias() : null;
        return generatePresignedPutUrl(alias, key, expiresIn, contentType);
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

    public String buildObjectUrl(String bucketAliasOrName, String key) {
        validateObjectKey(key);
        String bucket = bucketRegistry.resolve(bucketAliasOrName);
        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder().bucket(bucket).key(key).build())
                .toString();
    }

    public String buildObjectUrl(BucketAlias bucketAlias, String key) {
        String alias = bucketAlias != null ? bucketAlias.getAlias() : null;
        return buildObjectUrl(alias, key);
    }

    /**
     * Given a bare (non-presigned) S3/MinIO object URL previously built by {@link #buildObjectUrl},
     * extracts the object key from the URL path and generates a time-limited presigned GET URL.
     *
     * <p>Returns {@code null} if {@code bareUrl} is blank or cannot be parsed, so callers can
     * pass nullable stored URLs safely.</p>
     */
    public String presignedGetUrlFromBareUrl(BucketAlias bucketAlias, String bareUrl, Duration expiresIn) {
        String alias = bucketAlias != null ? bucketAlias.getAlias() : null;
        return presignedGetUrlFromBareUrl(alias, bareUrl, expiresIn);
    }

    public String presignedGetUrlFromBareUrl(String bucketAliasOrName, String bareUrl, Duration expiresIn) {
        if (!StringUtils.hasText(bareUrl)) {
            return null;
        }
        try {
            URI uri = new URI(bareUrl.trim());
            // S3/MinIO path is "/<bucket>/<key>" — strip the leading "/<bucket>/" segment
            String path = uri.getPath(); // e.g. "/shade-event-assets/events/uuid/cover/uuid"
            if (!StringUtils.hasText(path) || path.equals("/")) {
                return null;
            }
            // Remove leading slash, then skip the first segment (bucket name)
            String withoutLeadingSlash = path.startsWith("/") ? path.substring(1) : path;
            int firstSlash = withoutLeadingSlash.indexOf('/');
            if (firstSlash < 0) {
                return null; // no key after bucket segment
            }
            String objectKey = withoutLeadingSlash.substring(firstSlash + 1);
            if (!StringUtils.hasText(objectKey)) {
                return null;
            }
            return generatePresignedGetUrl(bucketAliasOrName, objectKey, expiresIn).toString();
        } catch (Exception e) {
            return null;
        }
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

