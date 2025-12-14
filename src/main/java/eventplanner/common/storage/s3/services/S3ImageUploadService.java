package eventplanner.common.storage.s3.services;

import eventplanner.common.exception.BadRequestException;
import eventplanner.common.storage.s3.dto.PresignedUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    private final S3StorageService storageService;

    public PresignedUploadResult createImageUpload(String bucketAlias,
                                                   String keyPrefix,
                                                   String fileName,
                                                   String contentType,
                                                   Duration expiresIn) {
        if (!storageService.isConfigured()) {
            throw new BadRequestException("S3_NOT_CONFIGURED", "Image uploads are not configured");
        }
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.US).startsWith("image/")) {
            throw new BadRequestException("INVALID_CONTENT_TYPE", "Only image uploads are allowed");
        }

        String sanitizedFileName = sanitizeFileName(fileName);
        String extension = determineExtension(contentType, sanitizedFileName);
        String objectKey = buildObjectKey(keyPrefix, extension);

        URL uploadUrl;
        try {
            uploadUrl = storageService.generatePresignedPutUrl(
                bucketAlias,
                objectKey,
                expiresIn,
                contentType
            );
        } catch (IllegalStateException ex) {
            throw new BadRequestException("S3_NOT_CONFIGURED", ex.getMessage());
        }

        String resourceUrl = storageService.stripQuery(uploadUrl);

        return PresignedUploadResult.builder()
            .uploadMethod("PUT")
            .uploadUrl(uploadUrl.toString())
            .headers(Map.of("Content-Type", contentType))
            .objectKey(objectKey)
            .resourceUrl(resourceUrl)
            .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(expiresIn))
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
            throw new BadRequestException("INVALID_PROFILE_IMAGE_URL", "Invalid profile image URL");
        }
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
}


