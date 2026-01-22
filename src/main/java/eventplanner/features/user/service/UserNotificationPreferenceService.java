package eventplanner.features.user.service;

import eventplanner.features.user.entity.UserNotificationPreference;
import eventplanner.features.user.repository.UserNotificationPreferenceRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user notification preferences.
 * Handles granular notification control per notification type and communication channel.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserNotificationPreferenceService {

    private final UserNotificationPreferenceRepository notificationPreferenceRepository;
    private final UserAccountRepository userAccountRepository;

    /**
     * Get all notification preferences for a user
     */
    public List<UserNotificationPreference> getUserNotificationPreferences(UUID userId) {
        log.debug("Fetching notification preferences for user: {}", userId);
        return notificationPreferenceRepository.findByUserId(userId);
    }

    /**
     * Get notification preference for specific type and channel
     */
    public Optional<UserNotificationPreference> getNotificationPreference(
        UUID userId,
        String notificationType,
        String channel
    ) {
        log.debug("Fetching notification preference for user: {}, type: {}, channel: {}",
            userId, notificationType, channel);

        return notificationPreferenceRepository.findByUserIdAndTypeAndChannel(
            userId, notificationType, channel
        );
    }

    /**
     * Check if notification is enabled for user (type + channel)
     * Returns true by default if preference not set (opt-out model)
     */
    public boolean isNotificationEnabled(UUID userId, String notificationType, String channel) {
        log.debug("Checking if notification is enabled for user: {}, type: {}, channel: {}",
            userId, notificationType, channel);

        return notificationPreferenceRepository.isEnabled(userId, notificationType, channel)
            .orElse(true); // Default: enabled (opt-out model)
    }

    /**
     * Set notification preference (create or update)
     */
    public UserNotificationPreference setNotificationPreference(
        UUID userId,
        String notificationType,
        String channel,
        Boolean enabled,
        String frequency
    ) {
        validateInputs(notificationType, channel);

        log.info("Setting notification preference for user: {}, type: {}, channel: {}, enabled: {}",
            userId, notificationType, channel, enabled);

        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Optional<UserNotificationPreference> existing =
            notificationPreferenceRepository.findByUserIdAndTypeAndChannel(userId, notificationType, channel);

        if (existing.isPresent()) {
            // Update existing preference
            UserNotificationPreference preference = existing.get();
            preference.setEnabled(enabled);
            if (frequency != null) {
                preference.setFrequency(frequency);
            }
            return notificationPreferenceRepository.save(preference);
        } else {
            // Create new preference
            UserNotificationPreference preference = new UserNotificationPreference();
            preference.setUser(user);
            preference.setNotificationType(notificationType);
            preference.setChannel(channel);
            preference.setEnabled(enabled);
            preference.setFrequency(frequency);
            return notificationPreferenceRepository.save(preference);
        }
    }

    /**
     * Enable notification for specific type and channel
     */
    public UserNotificationPreference enableNotification(
        UUID userId,
        String notificationType,
        String channel
    ) {
        return setNotificationPreference(userId, notificationType, channel, true, null);
    }

    /**
     * Disable notification for specific type and channel
     */
    public UserNotificationPreference disableNotification(
        UUID userId,
        String notificationType,
        String channel
    ) {
        return setNotificationPreference(userId, notificationType, channel, false, null);
    }

    /**
     * Enable all notifications of a specific type (across all channels)
     */
    public void enableAllNotificationsOfType(UUID userId, String notificationType) {
        log.info("Enabling all notifications of type '{}' for user: {}", notificationType, userId);

        // Common channels
        String[] channels = {"EMAIL", "PUSH", "SMS"};
        for (String channel : channels) {
            enableNotification(userId, notificationType, channel);
        }
    }

    /**
     * Disable all notifications of a specific type (across all channels)
     */
    public void disableAllNotificationsOfType(UUID userId, String notificationType) {
        log.info("Disabling all notifications of type '{}' for user: {}", notificationType, userId);

        String[] channels = {"EMAIL", "PUSH", "SMS"};
        for (String channel : channels) {
            disableNotification(userId, notificationType, channel);
        }
    }

    /**
     * Enable all notifications for a user (opt-in to everything)
     */
    public void enableAllNotifications(UUID userId) {
        log.info("Enabling all notifications for user: {}", userId);

        List<UserNotificationPreference> preferences = notificationPreferenceRepository.findByUserId(userId);
        preferences.forEach(pref -> pref.setEnabled(true));
        notificationPreferenceRepository.saveAll(preferences);
    }

    /**
     * Disable all notifications for a user (global opt-out)
     */
    public void disableAllNotifications(UUID userId) {
        log.info("Disabling all notifications for user: {}", userId);

        List<UserNotificationPreference> preferences = notificationPreferenceRepository.findByUserId(userId);
        preferences.forEach(pref -> pref.setEnabled(false));
        notificationPreferenceRepository.saveAll(preferences);
    }

    /**
     * Delete notification preference
     */
    public void deleteNotificationPreference(UUID userId, String notificationType, String channel) {
        log.info("Deleting notification preference for user: {}, type: {}, channel: {}",
            userId, notificationType, channel);

        UserNotificationPreference preference = notificationPreferenceRepository
            .findByUserIdAndTypeAndChannel(userId, notificationType, channel)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Notification preference not found for user: %s, type: %s, channel: %s",
                    userId, notificationType, channel)
            ));

        notificationPreferenceRepository.delete(preference);
    }

    /**
     * Delete all notification preferences for a user
     */
    public void deleteAllNotificationPreferences(UUID userId) {
        log.info("Deleting all notification preferences for user: {}", userId);

        List<UserNotificationPreference> preferences = notificationPreferenceRepository.findByUserId(userId);
        notificationPreferenceRepository.deleteAll(preferences);
    }

    /**
     * Validate notification type and channel inputs
     */
    private void validateInputs(String notificationType, String channel) {
        if (notificationType == null || notificationType.isBlank()) {
            throw new BadRequestException("Notification type cannot be empty");
        }
        if (channel == null || channel.isBlank()) {
            throw new BadRequestException("Channel cannot be empty");
        }

        // Validate channel is one of the supported channels
        String normalizedChannel = channel.toUpperCase();
        if (!normalizedChannel.equals("EMAIL") &&
            !normalizedChannel.equals("PUSH") &&
            !normalizedChannel.equals("SMS")) {
            throw new BadRequestException("Invalid channel. Supported channels: EMAIL, PUSH, SMS");
        }
    }
}
