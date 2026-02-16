package eventplanner.common.storage.s3.services;

import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.storage.s3.registry.BucketAlias;
import eventplanner.common.storage.s3.dto.PresignedUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for handling image-specific uploads to S3.
 * Provides validation, sanitization, and extension handling for image files.
 */
@Service
@RequiredArgsConstructor
public class S3ImageUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".heic", ".heif"
    );

    private static final Map<String, String> CONTENT_TYPE_EXTENSION_MAP = Map.of(
        "image/jpeg", ".jpg",
        "image/jpg", ".jpg",
        "image/png", ".png",
        "image/gif", ".gif",
        "image/webp", ".webp",
        "image/heic", ".heic",
        "image/heif", ".heif"
    );

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("(\\.\\./|\\.\\.\\\\)");
    private static final int MAX_KEY_PREFIX_LENGTH = 500;
    private static final Duration DEFAULT_UPLOAD_URL_TTL = Duration.ofMinutes(10);

    private final S3StorageService storageService;
    private final Clock clock;

    public PresignedUploadResult createImageUpload(BucketAlias bucketAlias,
                                                   String keyPrefix,
                                                   String fileName,
                                                   String contentType,
                                                   Duration expiresIn) {
        String alias = bucketAlias != null ? bucketAlias.getAlias() : null;
        return createImageUpload(alias, keyPrefix, fileName, contentType, expiresIn);
    }

    public PresignedUploadResult createImageUpload(String bucketAlias,
                                                   String keyPrefix,
                                                   String fileName,
                                                   String contentType,
                                                   Duration expiresIn) {
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.US).startsWith("image/")) {
            throw new BadRequestException("Only image uploads are allowed");
        }

        validateKeyPrefix(keyPrefix);
        String sanitizedFileName = sanitizeFileName(fileName);
        String extension = determineExtension(contentType, sanitizedFileName);
        
        // Validate that content-type matches the extension
        if (StringUtils.hasText(extension) && !isValidImageExtension(extension)) {
            throw new BadRequestException("Invalid image file extension: " + extension);
        }
        
        String objectKey = buildObjectKey(keyPrefix, extension);
        Duration ttl = normalizeTtl(expiresIn);

        URL uploadUrl;
        try {
            uploadUrl = storageService.generatePresignedPutUrl(
                bucketAlias,
                objectKey,
                ttl,
                contentType
            );
        } catch (IllegalStateException ex) {
            throw new BadRequestException(ex.getMessage());
        }

        String resourceUrl = storageService.stripQuery(uploadUrl);

        return PresignedUploadResult.builder()
            .uploadMethod("PUT")
            .uploadUrl(uploadUrl.toString())
            .headers(Map.of("Content-Type", contentType))
            .objectKey(objectKey)
            .resourceUrl(resourceUrl)
            .expiresAt(LocalDateTime.now(clock).plus(ttl))
            .build();
    }

    public String normalizeResourceUrl(String resourceUrl) {
        if (!StringUtils.hasText(resourceUrl)) {
            return null;
        }
        try {
            URL url = new URL(resourceUrl.trim());
            return storageService.stripQuery(url);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid profile image URL");
        }
    }

    public String buildResourceUrl(String bucketAlias, String objectKey) {
        return storageService.buildObjectUrl(bucketAlias, objectKey);
    }

    public String buildResourceUrl(BucketAlias bucketAlias, String objectKey) {
        return storageService.buildObjectUrl(bucketAlias, objectKey);
    }

    private Duration normalizeTtl(Duration expiresIn) {
        if (expiresIn == null || expiresIn.isZero() || expiresIn.isNegative()) {
            return DEFAULT_UPLOAD_URL_TTL;
        }
        return expiresIn;
    }

    private String sanitizeFileName(String fileName) {
        String cleaned = StringUtils.hasText(fileName) ? fileName.trim() : "image";
        cleaned = cleaned.replace("\\", "/");
        int lastSlash = cleaned.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < cleaned.length() - 1) {
            cleaned = cleaned.substring(lastSlash + 1);
        }
        cleaned = cleaned.replaceAll("\\s+", "_");
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!StringUtils.hasText(cleaned) || ".".equals(cleaned)) {
            cleaned = "image";
        }
        if (cleaned.length() > 120) {
            cleaned = cleaned.substring(0, 120);
        }
        return cleaned;
    }

    private String determineExtension(String contentType, String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            String rawExtension = fileName.substring(dotIndex).toLowerCase(Locale.US);
            if (ALLOWED_EXTENSIONS.contains(rawExtension)) {
                return ".jpeg".equals(rawExtension) ? ".jpg" : rawExtension;
            }
        }
        String normalizedType = contentType.toLowerCase(Locale.US);
        return CONTENT_TYPE_EXTENSION_MAP.getOrDefault(normalizedType, "");
    }

    private String buildObjectKey(String keyPrefix, String extension) {
        StringBuilder key = new StringBuilder();
        if (StringUtils.hasText(keyPrefix)) {
            key.append(keyPrefix.trim());
            if (!keyPrefix.endsWith("/")) {
                key.append('/');
            }
        }
        key.append(UUID.randomUUID());
        if (StringUtils.hasText(extension)) {
            key.append(extension);
        }
        return key.toString();
    }

    /**
     * Validates key prefix to prevent path traversal attacks.
     */
    private void validateKeyPrefix(String keyPrefix) {
        if (keyPrefix == null) {
            return; // null is allowed, will default to root
        }
        if (keyPrefix.length() > MAX_KEY_PREFIX_LENGTH) {
            throw new BadRequestException("Key prefix exceeds maximum length of " + MAX_KEY_PREFIX_LENGTH);
        }
        if (PATH_TRAVERSAL_PATTERN.matcher(keyPrefix).find()) {
            throw new BadRequestException("Key prefix contains path traversal sequences (../)");
        }
        if (keyPrefix.startsWith("/") || keyPrefix.contains("//")) {
            throw new BadRequestException("Key prefix cannot start with '/' or contain '//'");
        }
    }

    /**
     * Validates that the extension is in the allowed list.
     */
    private boolean isValidImageExtension(String extension) {
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.US));
    }
}


