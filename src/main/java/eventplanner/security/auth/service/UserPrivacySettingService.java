package eventplanner.security.auth.service;

import eventplanner.security.auth.entity.UserPrivacySetting;
import eventplanner.security.auth.repository.UserPrivacySettingRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user privacy settings.
 * Handles privacy controls (profile visibility, email visibility, etc.)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserPrivacySettingService {

    private final UserPrivacySettingRepository privacySettingRepository;
    private final UserAccountRepository userAccountRepository;

    // Common privacy setting keys
    public static final String PROFILE_VISIBILITY = "profile_visibility";
    public static final String EMAIL_VISIBLE = "email_visible";
    public static final String PHONE_VISIBLE = "phone_visible";
    public static final String EVENTS_VISIBLE = "events_visible";
    public static final String FOLLOWERS_VISIBLE = "followers_visible";
    public static final String FOLLOWING_VISIBLE = "following_visible";

    // Common privacy setting values
    public static final String PUBLIC = "PUBLIC";
    public static final String PRIVATE = "PRIVATE";
    public static final String FRIENDS_ONLY = "FRIENDS_ONLY";

    /**
     * Get all privacy settings for a user as a map
     */
    public Map<String, String> getUserPrivacySettings(UUID userId) {
        log.debug("Fetching privacy settings for user: {}", userId);

        List<UserPrivacySetting> settings = privacySettingRepository.findByUserId(userId);

        return settings.stream()
            .collect(Collectors.toMap(
                UserPrivacySetting::getSettingKey,
                UserPrivacySetting::getSettingValue
            ));
    }

    /**
     * Get a specific privacy setting value for a user
     */
    public Optional<String> getPrivacySetting(UUID userId, String key) {
        log.debug("Fetching privacy setting '{}' for user: {}", key, userId);

        return privacySettingRepository.findByUserIdAndKey(userId, key)
            .map(UserPrivacySetting::getSettingValue);
    }

    /**
     * Set a privacy setting (create or update)
     */
    public UserPrivacySetting setPrivacySetting(
        UUID userId,
        String key,
        String value,
        String description
    ) {
        validateInputs(key, value);

        log.info("Setting privacy setting '{}' = '{}' for user: {}", key, value, userId);

        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Optional<UserPrivacySetting> existing = privacySettingRepository.findByUserIdAndKey(userId, key);

        if (existing.isPresent()) {
            // Update existing setting
            UserPrivacySetting setting = existing.get();
            setting.setSettingValue(value);
            if (description != null) {
                setting.setDescription(description);
            }
            return privacySettingRepository.save(setting);
        } else {
            // Create new setting
            UserPrivacySetting setting = new UserPrivacySetting();
            setting.setUser(user);
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            setting.setDescription(description);
            return privacySettingRepository.save(setting);
        }
    }

    /**
     * Set multiple privacy settings at once
     */
    public void setPrivacySettings(UUID userId, Map<String, String> settings) {
        log.info("Setting {} privacy settings for user: {}", settings.size(), userId);

        for (Map.Entry<String, String> entry : settings.entrySet()) {
            setPrivacySetting(userId, entry.getKey(), entry.getValue(), null);
        }
    }

    /**
     * Set profile visibility (PUBLIC, PRIVATE, FRIENDS_ONLY)
     */
    public UserPrivacySetting setProfileVisibility(UUID userId, String visibility) {
        return setPrivacySetting(userId, PROFILE_VISIBILITY, visibility, "Who can view your profile");
    }

    /**
     * Set email visibility
     */
    public UserPrivacySetting setEmailVisibility(UUID userId, String visibility) {
        return setPrivacySetting(userId, EMAIL_VISIBLE, visibility, "Who can see your email address");
    }

    /**
     * Set phone visibility
     */
    public UserPrivacySetting setPhoneVisibility(UUID userId, String visibility) {
        return setPrivacySetting(userId, PHONE_VISIBLE, visibility, "Who can see your phone number");
    }

    /**
     * Set events visibility (who can see events you're attending)
     */
    public UserPrivacySetting setEventsVisibility(UUID userId, String visibility) {
        return setPrivacySetting(userId, EVENTS_VISIBLE, visibility, "Who can see events you're attending");
    }

    /**
     * Check if user has public profile
     */
    public boolean isProfilePublic(UUID userId) {
        return getPrivacySetting(userId, PROFILE_VISIBILITY)
            .map(value -> value.equals(PUBLIC))
            .orElse(true); // Default: public
    }

    /**
     * Check if user email is visible to a specific visibility level
     */
    public boolean isEmailVisible(UUID userId, String requestedVisibility) {
        String setting = getPrivacySetting(userId, EMAIL_VISIBLE).orElse(PRIVATE);
        return isVisibilityAllowed(setting, requestedVisibility);
    }

    /**
     * Make profile completely public (all settings to PUBLIC)
     */
    public void makeProfilePublic(UUID userId) {
        log.info("Making profile public for user: {}", userId);

        setProfileVisibility(userId, PUBLIC);
        setEmailVisibility(userId, PUBLIC);
        setPhoneVisibility(userId, PUBLIC);
        setEventsVisibility(userId, PUBLIC);
        setPrivacySetting(userId, FOLLOWERS_VISIBLE, PUBLIC, null);
        setPrivacySetting(userId, FOLLOWING_VISIBLE, PUBLIC, null);
    }

    /**
     * Make profile completely private (all settings to PRIVATE)
     */
    public void makeProfilePrivate(UUID userId) {
        log.info("Making profile private for user: {}", userId);

        setProfileVisibility(userId, PRIVATE);
        setEmailVisibility(userId, PRIVATE);
        setPhoneVisibility(userId, PRIVATE);
        setEventsVisibility(userId, PRIVATE);
        setPrivacySetting(userId, FOLLOWERS_VISIBLE, PRIVATE, null);
        setPrivacySetting(userId, FOLLOWING_VISIBLE, PRIVATE, null);
    }

    /**
     * Delete a privacy setting
     */
    public void deletePrivacySetting(UUID userId, String key) {
        log.info("Deleting privacy setting '{}' for user: {}", key, userId);

        UserPrivacySetting setting = privacySettingRepository.findByUserIdAndKey(userId, key)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Privacy setting '%s' not found for user: %s", key, userId)
            ));

        privacySettingRepository.delete(setting);
    }

    /**
     * Delete all privacy settings for a user
     */
    public void deleteAllPrivacySettings(UUID userId) {
        log.info("Deleting all privacy settings for user: {}", userId);

        List<UserPrivacySetting> settings = privacySettingRepository.findByUserId(userId);
        privacySettingRepository.deleteAll(settings);
    }

    /**
     * Get privacy setting with default value
     */
    public String getPrivacySettingOrDefault(UUID userId, String key, String defaultValue) {
        return getPrivacySetting(userId, key).orElse(defaultValue);
    }

    /**
     * Check if visibility is allowed based on setting and requested level
     */
    private boolean isVisibilityAllowed(String setting, String requestedLevel) {
        if (setting.equals(PUBLIC)) {
            return true;
        }
        if (setting.equals(PRIVATE)) {
            return requestedLevel.equals(PRIVATE);
        }
        if (setting.equals(FRIENDS_ONLY)) {
            return requestedLevel.equals(PRIVATE) || requestedLevel.equals(FRIENDS_ONLY);
        }
        return false;
    }

    /**
     * Validate privacy setting inputs
     */
    private void validateInputs(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new BadRequestException("Privacy setting key cannot be empty");
        }
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Privacy setting value cannot be empty");
        }

        // Validate value is one of the supported visibility levels
        String normalizedValue = value.toUpperCase();
        if (!normalizedValue.equals(PUBLIC) &&
            !normalizedValue.equals(PRIVATE) &&
            !normalizedValue.equals(FRIENDS_ONLY)) {
            throw new BadRequestException(
                "Invalid privacy setting value. Supported values: PUBLIC, PRIVATE, FRIENDS_ONLY"
            );
        }
    }
}
