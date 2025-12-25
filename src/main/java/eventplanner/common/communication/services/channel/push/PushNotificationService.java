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
        Optional<DeviceToken> tokenToRefresh = Optional.empty();
        
        if (request.getOldDeviceToken() != null && !request.getOldDeviceToken().isEmpty()) {
            tokenToRefresh = deviceTokenRepository.findByDeviceToken(request.getOldDeviceToken());
        } else {
            List<DeviceToken> activeTokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(request.getUserId());
            if (!activeTokens.isEmpty()) {
                tokenToRefresh = activeTokens.stream()
                        .filter(token -> token.getLastUsedAt() != null)
                        .max((t1, t2) -> t1.getLastUsedAt().compareTo(t2.getLastUsedAt()));
                if (tokenToRefresh.isEmpty() && !activeTokens.isEmpty()) {
                    tokenToRefresh = Optional.of(activeTokens.get(0));
                }
            }
        }
        
        if (tokenToRefresh.isPresent() && tokenToRefresh.get().getUserId().equals(request.getUserId())) {
            DeviceToken token = tokenToRefresh.get();
            
            Optional<DeviceToken> existingNewToken = deviceTokenRepository.findByDeviceToken(request.getDeviceToken());
            if (existingNewToken.isPresent() && !existingNewToken.get().getId().equals(token.getId())) {
                token.deactivate();
                deviceTokenRepository.save(token);
                return existingNewToken.get();
            }
            
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
            
            return deviceTokenRepository.save(token);
        }
        
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
        Optional<DeviceToken> existingToken = deviceTokenRepository.findByDeviceToken(request.getDeviceToken());
        
        if (existingToken.isPresent()) {
            DeviceToken token = existingToken.get();
            if (token.getUserId().equals(request.getUserId())) {
                token.setPlatform(request.getPlatform());
                token.setDeviceId(request.getDeviceId());
                token.setAppVersion(request.getAppVersion());
                token.setIsActive(true);
                token.markAsUsed();
                return deviceTokenRepository.save(token);
            } else {
                token.deactivate();
                deviceTokenRepository.save(token);
            }
        }

        DeviceToken newToken = new DeviceToken();
        newToken.setUserId(request.getUserId());
        newToken.setDeviceToken(request.getDeviceToken());
        newToken.setPlatform(request.getPlatform());
        newToken.setDeviceId(request.getDeviceId());
        newToken.setAppVersion(request.getAppVersion());
        newToken.setIsActive(true);
        newToken.setLastUsedAt(LocalDateTime.now());

        return deviceTokenRepository.save(newToken);
    }

    /**
     * Deactivate all tokens for a user
     */
    public void deactivateAllUserTokens(UUID userId) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
        tokens.forEach(DeviceToken::deactivate);
        deviceTokenRepository.saveAll(tokens);
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
     */
    public PushResult sendToNotification(UUID userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging.isEmpty()) {
            return PushResult.builder()
                    .success(false)
                    .errorMessage("Firebase Messaging is not configured")
                    .build();
        }

        try {
            List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
            
            if (tokens.isEmpty()) {
                return PushResult.builder()
                        .success(false)
                        .errorMessage("No active device tokens found for user")
                        .build();
            }

            FirebaseMessaging messaging = firebaseMessaging.get();
            String lastMessageId = null;

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

                    if (data != null && !data.isEmpty()) {
                        messageBuilder.putAllData(data);
                    }

                    Message message = messageBuilder.build();
                    String response = messaging.send(message);
                    lastMessageId = response;
                    
                    updateLastUsed(token.getDeviceToken());
                            
                } catch (FirebaseMessagingException e) {
                    if (e.getErrorCode().equals("invalid-argument") || 
                        e.getErrorCode().equals("registration-token-not-registered")) {
                        token.deactivate();
                        deviceTokenRepository.save(token);
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
            return PushResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
