package eventplanner.common.communication.services.channel.push;

import eventplanner.common.communication.services.channel.push.dto.PushNotificationRequest;
import eventplanner.common.communication.services.channel.push.dto.PushNotificationResponse;
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

import java.util.HashMap;
import java.util.Map;
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
     * 
     * @param userId User ID
     * @param title Notification title
     * @param data Map of data (can include "body" and "actionUrl" keys)
     * @param eventId Optional event ID
     * @return PushResult with send status
     */
    public PushResult sendPushNotification(UUID userId, String title, Map<String, Object> data, UUID eventId) {
        try {
            // Extract actionUrl and body if present in data
            String actionUrl = null;
            String body = title; // Default to title as body
            if (data != null) {
                if (data.containsKey("actionUrl")) {
                    Object actionUrlObj = data.get("actionUrl");
                    if (actionUrlObj != null) {
                        actionUrl = actionUrlObj.toString();
                    }
                }
                if (data.containsKey("body")) {
                    Object bodyObj = data.get("body");
                    if (bodyObj != null) {
                        body = bodyObj.toString();
                    }
                }
            }
            
            // Convert data Map<String, Object> to Map<String, String> for push notification
            // Exclude actionUrl and body as they're handled separately
            Map<String, String> pushData = null;
            if (data != null && !data.isEmpty()) {
                pushData = new HashMap<>();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String key = entry.getKey();
                    // Skip actionUrl and body as they're handled separately
                    if (!"actionUrl".equals(key) && !"body".equals(key)) {
                        pushData.put(key, entry.getValue() != null ? entry.getValue().toString() : "");
                    }
                }
            }
            
            // Build push notification request
            PushNotificationRequest pushRequest = PushNotificationRequest.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .title(title)
                    .body(body)
                    .data(pushData)
                    .actionUrl(actionUrl)
                    .build();
            
            // Send push notification
            PushNotificationResponse response = sendToUser(pushRequest);
            
            // Convert response to PushResult
            String messageId = response.getNotificationId() != null 
                    ? response.getNotificationId().toString() 
                    : null;
            
            String errorMessage = null;
            if (!response.isSuccess()) {
                errorMessage = response.getMessage();
                if (response.getFailedCount() > 0) {
                    errorMessage += String.format(" (%d failed, %d sent)", 
                            response.getFailedCount(), response.getSentCount());
                }
            }
            
            return PushResult.builder()
                    .success(response.isSuccess())
                    .messageId(messageId)
                    .errorMessage(errorMessage)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to send push notification: {}", e.getMessage(), e);
            return PushResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
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

