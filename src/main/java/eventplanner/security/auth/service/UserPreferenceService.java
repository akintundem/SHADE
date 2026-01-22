package eventplanner.security.auth.service;

import eventplanner.security.auth.entity.UserPreference;
import eventplanner.security.auth.repository.UserPreferenceRepository;
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
 * Service for managing user preferences.
 * Handles CRUD operations for normalized user preferences (theme, language, timezone, etc.)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserAccountRepository userAccountRepository;

    /**
     * Get all preferences for a user as a map
     */
    public Map<String, String> getUserPreferences(UUID userId) {
        log.debug("Fetching preferences for user: {}", userId);

        List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);

        return preferences.stream()
            .collect(Collectors.toMap(
                UserPreference::getPreferenceKey,
                UserPreference::getPreferenceValue
            ));
    }

    /**
     * Get a specific preference value for a user
     */
    public Optional<String> getUserPreference(UUID userId, String key) {
        log.debug("Fetching preference '{}' for user: {}", key, userId);

        return userPreferenceRepository.findByUserIdAndKey(userId, key)
            .map(UserPreference::getPreferenceValue);
    }

    /**
     * Set a user preference (create or update)
     */
    public UserPreference setUserPreference(UUID userId, String key, String value, String description) {
        if (key == null || key.isBlank()) {
            throw new BadRequestException("Preference key cannot be empty");
        }

        log.info("Setting preference '{}' = '{}' for user: {}", key, value, userId);

        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Optional<UserPreference> existing = userPreferenceRepository.findByUserIdAndKey(userId, key);

        if (existing.isPresent()) {
            // Update existing preference
            UserPreference preference = existing.get();
            preference.setPreferenceValue(value);
            if (description != null) {
                preference.setDescription(description);
            }
            return userPreferenceRepository.save(preference);
        } else {
            // Create new preference
            UserPreference preference = new UserPreference();
            preference.setUser(user);
            preference.setPreferenceKey(key);
            preference.setPreferenceValue(value);
            preference.setDescription(description);
            return userPreferenceRepository.save(preference);
        }
    }

    /**
     * Set multiple user preferences at once
     */
    public void setUserPreferences(UUID userId, Map<String, String> preferences) {
        log.info("Setting {} preferences for user: {}", preferences.size(), userId);

        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        for (Map.Entry<String, String> entry : preferences.entrySet()) {
            setUserPreference(userId, entry.getKey(), entry.getValue(), null);
        }
    }

    /**
     * Delete a user preference
     */
    public void deleteUserPreference(UUID userId, String key) {
        log.info("Deleting preference '{}' for user: {}", key, userId);

        UserPreference preference = userPreferenceRepository.findByUserIdAndKey(userId, key)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Preference '%s' not found for user: %s", key, userId)
            ));

        userPreferenceRepository.delete(preference);
    }

    /**
     * Delete all preferences for a user
     */
    public void deleteAllUserPreferences(UUID userId) {
        log.info("Deleting all preferences for user: {}", userId);

        List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);
        userPreferenceRepository.deleteAll(preferences);
    }

    /**
     * Check if a user has a specific preference set
     */
    public boolean hasUserPreference(UUID userId, String key) {
        return userPreferenceRepository.existsByUserIdAndKey(userId, key);
    }

    /**
     * Get user preference with default value
     */
    public String getUserPreferenceOrDefault(UUID userId, String key, String defaultValue) {
        return getUserPreference(userId, key).orElse(defaultValue);
    }

    /**
     * Get all preferences for a user as entities (for internal use)
     */
    public List<UserPreference> getUserPreferenceEntities(UUID userId) {
        return userPreferenceRepository.findByUserId(userId);
    }
}
