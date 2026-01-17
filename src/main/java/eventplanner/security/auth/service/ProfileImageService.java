package eventplanner.security.auth.service;

import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.storage.s3.dto.PresignedUploadResult;
import eventplanner.common.storage.s3.services.S3ImageUploadService;
import eventplanner.security.auth.dto.req.ProfileImageCompleteRequest;
import eventplanner.security.auth.dto.req.ProfileImageUploadRequest;
import eventplanner.security.auth.dto.res.ProfileImageCompleteResponse;
import eventplanner.security.auth.dto.res.ProfileImageUploadResponse;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final String USER_BUCKET_ALIAS = "user";
    private static final String PROFILE_KEY_PREFIX_TEMPLATE = "users/%s/profile";

    private final S3ImageUploadService imageUploadService;
    private final UserAccountRepository userAccountRepository;

    public ProfileImageUploadResponse createProfileImageUpload(UserAccount user, ProfileImageUploadRequest request) {
        if (user == null) {
            throw new BadRequestException("Valid user is required for profile uploads");
        }

        String keyPrefix = PROFILE_KEY_PREFIX_TEMPLATE.formatted(user.getId());
        PresignedUploadResult result = imageUploadService.createImageUpload(
            USER_BUCKET_ALIAS,
            keyPrefix,
            request.getFileName(),
            request.getContentType(),
            UPLOAD_URL_TTL
        );

        return ProfileImageUploadResponse.builder()
            .uploadMethod(result.getUploadMethod())
            .uploadUrl(result.getUploadUrl())
            .headers(result.getHeaders())
            .objectKey(result.getObjectKey())
            .resourceUrl(result.getResourceUrl())
            .expiresAt(result.getExpiresAt())
            .build();
    }

    public String normalizeResourceUrl(String resourceUrl) {
        return imageUploadService.normalizeResourceUrl(resourceUrl);
    }

    /**
     * Called AFTER the client uploads directly to S3 using the presigned URL.
     * Persists the resulting resourceUrl on the user record.
     */
    public ProfileImageCompleteResponse completeProfileImageUpload(UserAccount user, ProfileImageCompleteRequest request) {
        if (user == null) {
            throw new BadRequestException("Valid user is required");
        }
        if (request == null) {
            throw new BadRequestException("Request is required");
        }

        String expectedPrefix = (PROFILE_KEY_PREFIX_TEMPLATE.formatted(user.getId())) + "/";
        String objectKey = request.getObjectKey() != null ? request.getObjectKey().trim() : "";
        if (!StringUtils.hasText(objectKey) || !objectKey.startsWith(expectedPrefix)) {
            throw new BadRequestException("Invalid object key for this user");
        }

        String normalized = imageUploadService.normalizeResourceUrl(request.getResourceUrl());
        if (!StringUtils.hasText(normalized)) {
            throw new BadRequestException("Invalid resourceUrl");
        }

        user.setProfilePictureUrl(normalized);
        userAccountRepository.save(user);

        return ProfileImageCompleteResponse.builder()
            .profilePictureUrl(normalized)
            .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
            .build();
    }
}
