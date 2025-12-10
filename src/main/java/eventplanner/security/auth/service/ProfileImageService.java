package eventplanner.security.auth.service;

import eventplanner.common.exception.BadRequestException;
import eventplanner.common.storage.s3.PresignedUploadResult;
import eventplanner.common.storage.s3.S3ImageUploadService;
import eventplanner.security.auth.dto.req.ProfileImageUploadRequest;
import eventplanner.security.auth.dto.res.ProfileImageUploadResponse;
import eventplanner.security.auth.entity.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final String USER_BUCKET_ALIAS = "user";
    private static final String PROFILE_KEY_PREFIX_TEMPLATE = "users/%s/profile";

    private final S3ImageUploadService imageUploadService;

    public ProfileImageUploadResponse createProfileImageUpload(UserAccount user, ProfileImageUploadRequest request) {
        if (user == null) {
            throw new BadRequestException("USER_REQUIRED", "Valid user is required for profile uploads");
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
}
