package eventplanner.common.communication.services.channel.push;

import eventplanner.common.communication.services.channel.push.dto.RefreshDeviceTokenRequest;
import eventplanner.common.communication.services.channel.push.dto.RegisterDeviceTokenRequest;
import eventplanner.common.communication.services.core.dto.PushResult;
import eventplanner.common.communication.model.DeviceToken;
import eventplanner.common.communication.repository.DeviceTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for sending FCM push notifications and managing device tokens
 */
@Service
@Slf4j
@Transactional
public class PushNotificationService {

    private final Optional<FirebaseMessaging> firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    @Autowired
    public PushNotificationService(DeviceTokenRepository deviceTokenRepository,
                                   Optional<FirebaseMessaging> firebaseMessaging) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.firebaseMessaging = firebaseMessaging;
    }

    /**
     * Refresh an existing device token with a new token value
     */
    public DeviceToken refreshDeviceToken(RefreshDeviceTokenRequest request) {
        // Find the token to refresh
        Optional<DeviceToken> tokenToRefresh = Optional.empty();
        
        if (request.getOldDeviceToken() != null && !request.getOldDeviceToken().isEmpty()) {
            // If old token is provided, find by old token
            tokenToRefresh = deviceTokenRepository.findByDeviceToken(request.getOldDeviceToken());
        } else {
            // Otherwise, find the most recently used active token for the user
            List<DeviceToken> activeTokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(request.getUserId());
            if (!activeTokens.isEmpty()) {
                // Sort by lastUsedAt descending, get the most recently used one
                tokenToRefresh = activeTokens.stream()
                        .filter(token -> token.getLastUsedAt() != null)
                        .max((t1, t2) -> t1.getLastUsedAt().compareTo(t2.getLastUsedAt()));
                // If no token has lastUsedAt, take the first one
                if (tokenToRefresh.isEmpty() && !activeTokens.isEmpty()) {
                    tokenToRefresh = Optional.of(activeTokens.get(0));
                }
            }
        }
        
        // If old token exists and belongs to the user, update it
        if (tokenToRefresh.isPresent() && tokenToRefresh.get().getUserId().equals(request.getUserId())) {
            DeviceToken token = tokenToRefresh.get();
            
            // Check if new token already exists
            Optional<DeviceToken> existingNewToken = deviceTokenRepository.findByDeviceToken(request.getDeviceToken());
            if (existingNewToken.isPresent() && !existingNewToken.get().getId().equals(token.getId())) {
                // New token belongs to another record, deactivate old token and return existing
                token.deactivate();
                deviceTokenRepository.save(token);
                log.info("Old token deactivated, returning existing token record for user: {}", request.getUserId());
                return existingNewToken.get();
            }
            
            // Update the token with new value
            token.setDeviceToken(request.getDeviceToken());
            if (request.getPlatform() != null) {
                token.setPlatform(request.getPlatform());
            }
            if (request.getDeviceId() != null) {
                token.setDeviceId(request.getDeviceId());
            }
            if (request.getAppVersion() != null) {
                token.setAppVersion(request.getAppVersion());
            }
            token.setIsActive(true);
            token.markAsUsed();
            
            log.info("Refreshed device token for user: {}", request.getUserId());
            return deviceTokenRepository.save(token);
        }
        
        // If no matching token found, treat it as a new registration
        log.info("No matching token found for refresh, creating new token for user: {}", request.getUserId());
        DeviceToken newToken = new DeviceToken();
        newToken.setUserId(request.getUserId());
        newToken.setDeviceToken(request.getDeviceToken());
        newToken.setPlatform(request.getPlatform() != null ? request.getPlatform() : DeviceToken.Platform.ANDROID);
        newToken.setDeviceId(request.getDeviceId());
        newToken.setAppVersion(request.getAppVersion());
        newToken.setIsActive(true);
        newToken.setLastUsedAt(LocalDateTime.now());
        
        return deviceTokenRepository.save(newToken);
    }

    /**
     * Register or update a device token for a user
     */
    public DeviceToken registerDeviceToken(RegisterDeviceTokenRequest request) {
        // Check if token already exists
        Optional<DeviceToken> existingToken = deviceTokenRepository.findByDeviceToken(request.getDeviceToken());
        
        if (existingToken.isPresent()) {
            DeviceToken token = existingToken.get();
            // If the token belongs to the same user, update it
            if (token.getUserId().equals(request.getUserId())) {
                token.setPlatform(request.getPlatform());
                token.setDeviceId(request.getDeviceId());
                token.setAppVersion(request.getAppVersion());
                token.setIsActive(true);
                token.markAsUsed();
                log.info("Updated existing device token for user: {}", request.getUserId());
                return deviceTokenRepository.save(token);
            } else {
                // Token belongs to a different user, deactivate old one and create new
                token.deactivate();
                deviceTokenRepository.save(token);
                log.info("Device token transferred from user {} to user {}", 
                    token.getUserId(), request.getUserId());
            }
        }

        // Create new device token
        DeviceToken newToken = new DeviceToken();
        newToken.setUserId(request.getUserId());
        newToken.setDeviceToken(request.getDeviceToken());
        newToken.setPlatform(request.getPlatform());
        newToken.setDeviceId(request.getDeviceId());
        newToken.setAppVersion(request.getAppVersion());
        newToken.setIsActive(true);
        newToken.setLastUsedAt(LocalDateTime.now());

        log.info("Registered new device token for user: {}, platform: {}", 
            request.getUserId(), request.getPlatform());
        return deviceTokenRepository.save(newToken);
    }

    /**
     * Deactivate all tokens for a user
     */
    public void deactivateAllUserTokens(UUID userId) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
        tokens.forEach(DeviceToken::deactivate);
        deviceTokenRepository.saveAll(tokens);
        log.info("Deactivated all device tokens for user: {}", userId);
    }

    /**
     * Update last used timestamp for a device token
     */
    public void updateLastUsed(String deviceToken) {
        Optional<DeviceToken> token = deviceTokenRepository.findByDeviceToken(deviceToken);
        if (token.isPresent()) {
            token.get().markAsUsed();
            deviceTokenRepository.save(token.get());
        }
    }

    /**
     * Send push notification to a specific user
     * Simple method to send notification to all active device tokens for a user
     * 
     * @param userId The user ID
     * @param title Notification title
     * @param body Notification body (optional, can be null)
     * @param data Optional data payload (key-value pairs)
     * @return PushResult indicating success or failure
     */
    public PushResult sendToNotification(UUID userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging.isEmpty()) {
            log.warn("Firebase Messaging is not configured. Push notification not sent.");
            return PushResult.builder()
                    .success(false)
                    .errorMessage("Firebase Messaging is not configured")
                    .build();
        }

        try {
            // Get all active device tokens for the user
            List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
            
            if (tokens.isEmpty()) {
                log.warn("No active device tokens found for user: {}", userId);
                return PushResult.builder()
                        .success(false)
                        .errorMessage("No active device tokens found for user")
                        .build();
            }

            FirebaseMessaging messaging = firebaseMessaging.get();
            String lastMessageId = null;

            // Send notification to each device token
            for (DeviceToken token : tokens) {
                try {
                    Notification.Builder notificationBuilder = Notification.builder()
                            .setTitle(title);
                    
                    if (body != null && !body.isEmpty()) {
                        notificationBuilder.setBody(body);
                    }

                    Message.Builder messageBuilder = Message.builder()
                            .setToken(token.getDeviceToken())
                            .setNotification(notificationBuilder.build());

                    // Add data payload if provided
                    if (data != null && !data.isEmpty()) {
                        messageBuilder.putAllData(data);
                    }

                    Message message = messageBuilder.build();
                    String response = messaging.send(message);
                    lastMessageId = response;
                    
                    // Update last used timestamp
                    updateLastUsed(token.getDeviceToken());
                    
                    log.info("Successfully sent push notification to user {} device: {}, messageId: {}", 
                            userId, token.getDeviceToken(), response);
                            
                } catch (FirebaseMessagingException e) {
                    log.error("Failed to send push notification to device token: {}, error: {}", 
                            token.getDeviceToken(), e.getMessage());
                    
                    // If token is invalid, deactivate it
                    if (e.getErrorCode().equals("invalid-argument") || 
                        e.getErrorCode().equals("registration-token-not-registered")) {
                        token.deactivate();
                        deviceTokenRepository.save(token);
                        log.info("Deactivated invalid device token: {}", token.getDeviceToken());
                    }
                }
            }

            if (lastMessageId != null) {
                return PushResult.builder()
                        .success(true)
                        .messageId(lastMessageId)
                        .build();
            } else {
                return PushResult.builder()
                        .success(false)
                        .errorMessage("Failed to send notification to any device")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error sending push notification to user {}: {}", userId, e.getMessage(), e);
            return PushResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}

