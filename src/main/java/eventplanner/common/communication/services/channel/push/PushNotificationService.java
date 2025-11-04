package eventplanner.common.communication.services.channel.push;

import eventplanner.common.communication.services.channel.push.dto.PushNotificationRequest;
import eventplanner.common.communication.services.channel.push.dto.PushNotificationResponse;
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

import java.util.Optional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for sending FCM push notifications
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
     * Send push notification to a specific user
     */
    public PushNotificationResponse sendToUser(PushNotificationRequest request) {
        if (firebaseMessaging.isEmpty()) {
            log.warn("Firebase is not configured. Push notification not sent.");
            return PushNotificationResponse.builder()
                .success(false)
                .message("Firebase is not configured")
                .sentAt(LocalDateTime.now())
                .build();
        }

        List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(request.getUserId());
        
        if (deviceTokens.isEmpty()) {
            log.info("No active device tokens found for user: {}", request.getUserId());
            return PushNotificationResponse.builder()
                .success(false)
                .message("No active device tokens found for user")
                .sentCount(0)
                .failedCount(0)
                .sentAt(LocalDateTime.now())
                .build();
        }

        return sendToDevices(request, deviceTokens);
    }

    /**
     * Send push notification to specific device tokens
     */
    public PushNotificationResponse sendToDevices(PushNotificationRequest request, List<DeviceToken> deviceTokens) {
        if (firebaseMessaging.isEmpty()) {
            log.warn("Firebase is not configured. Push notification not sent.");
            return PushNotificationResponse.builder()
                .success(false)
                .message("Firebase is not configured")
                .sentAt(LocalDateTime.now())
                .build();
        }

        FirebaseMessaging messaging = firebaseMessaging.get();

        List<String> failedTokens = new ArrayList<>();
        int sentCount = 0;
        int failedCount = 0;

        for (DeviceToken deviceToken : deviceTokens) {
            try {
                Message message = buildMessage(request, deviceToken);
                String response = messaging.send(message);
                log.info("Push notification sent successfully to device: {}, response: {}", 
                    deviceToken.getId(), response);
                
                deviceToken.markAsUsed();
                deviceTokenRepository.save(deviceToken);
                sentCount++;
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send push notification to device: {}, error: {}", 
                    deviceToken.getId(), e.getMessage());
                
                // Handle invalid tokens
                if (isInvalidTokenError(e)) {
                    log.warn("Device token is invalid, deactivating: {}", deviceToken.getDeviceToken());
                    deviceToken.deactivate();
                    deviceTokenRepository.save(deviceToken);
                }
                
                failedTokens.add(deviceToken.getDeviceToken());
                failedCount++;
            } catch (Exception e) {
                log.error("Unexpected error sending push notification to device: {}", 
                    deviceToken.getId(), e);
                failedTokens.add(deviceToken.getDeviceToken());
                failedCount++;
            }
        }

        return PushNotificationResponse.builder()
            .success(sentCount > 0)
            .message(sentCount > 0 
                ? String.format("Sent %d notification(s), %d failed", sentCount, failedCount)
                : "Failed to send notifications")
            .notificationId(UUID.randomUUID())
            .sentCount(sentCount)
            .failedCount(failedCount)
            .failedTokens(failedTokens.isEmpty() ? null : failedTokens)
            .sentAt(LocalDateTime.now())
            .build();
    }

    /**
     * Build FCM message from request
     */
    private Message buildMessage(PushNotificationRequest request, DeviceToken deviceToken) {
        Message.Builder messageBuilder = Message.builder()
            .setToken(deviceToken.getDeviceToken())
            .setNotification(Notification.builder()
                .setTitle(request.getTitle())
                .setBody(request.getBody())
                .setImage(request.getImageUrl())
                .build());

        // Add custom data payload
        if (request.getData() != null && !request.getData().isEmpty()) {
            messageBuilder.putAllData(request.getData());
        }

        // Add event ID if provided
        if (request.getEventId() != null) {
            messageBuilder.putData("eventId", request.getEventId().toString());
        }

        // Add user ID
        if (request.getUserId() != null) {
            messageBuilder.putData("userId", request.getUserId().toString());
        }

        // Add action URL if provided
        if (request.getActionUrl() != null) {
            messageBuilder.putData("actionUrl", request.getActionUrl());
        }

        // Set platform-specific options
        if (deviceToken.getPlatform() == DeviceToken.Platform.ANDROID) {
            messageBuilder.setAndroidConfig(com.google.firebase.messaging.AndroidConfig.builder()
                .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
                .build());
        } else if (deviceToken.getPlatform() == DeviceToken.Platform.IOS) {
            messageBuilder.setApnsConfig(com.google.firebase.messaging.ApnsConfig.builder()
                .setAps(com.google.firebase.messaging.Aps.builder()
                    .setSound("default")
                    .setBadge(1)
                    .build())
                .build());
        }

        return messageBuilder.build();
    }

    /**
     * Check if error code indicates an invalid token
     */
    private boolean isInvalidTokenError(FirebaseMessagingException e) {
        com.google.firebase.messaging.MessagingErrorCode errorCode = e.getMessagingErrorCode();
        String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (errorCode != null) {
            String errorCodeStr = errorCode.toString();
            if (errorCodeStr.equals("INVALID_ARGUMENT") ||
                errorCodeStr.equals("UNREGISTERED") ||
                errorCodeStr.equals("NOT_FOUND")) {
                return true;
            }
        }
        
        return errorMessage.contains("registration-token-not-registered") ||
               errorMessage.contains("invalid registration token") ||
               errorMessage.contains("unregistered");
    }
}

