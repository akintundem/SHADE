package ai.eventplanner.pushnotification.service;

import ai.eventplanner.comms.model.DeviceToken;
import ai.eventplanner.comms.repository.DeviceTokenRepository;
import ai.eventplanner.pushnotification.dto.PushNotificationRequest;
import ai.eventplanner.pushnotification.dto.PushNotificationResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Firebase push notification service
 */
@Service
public class FirebasePushNotificationService {
    
    private final FirebaseMessaging firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;
    
    public FirebasePushNotificationService(FirebaseMessaging firebaseMessaging,
                                         DeviceTokenRepository deviceTokenRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.deviceTokenRepository = deviceTokenRepository;
    }
    
    /**
     * Send push notification to a specific user
     */
    public PushNotificationResponse sendToUser(PushNotificationRequest request) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(request.getUserId());
        return sendToTokens(tokens, request);
    }
    
    /**
     * Send push notification to all attendees of an event
     */
    public PushNotificationResponse sendToEvent(PushNotificationRequest request) {
        // Get all attendees' device tokens for the event
        // This would require a query to get all attendees of the event
        // For now, we'll use a placeholder - you'll need to implement this based on your event-attendee relationship
        List<DeviceToken> tokens = new ArrayList<>(); // TODO: Implement event attendee token retrieval
        return sendToTokens(tokens, request);
    }
    
    /**
     * Send push notification to multiple users
     */
    public PushNotificationResponse sendToUsers(List<UUID> userIds, PushNotificationRequest request) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdInAndIsActiveTrue(userIds);
        return sendToTokens(tokens, request);
    }
    
    /**
     * Send push notification to all active device tokens
     */
    public PushNotificationResponse sendToAll(PushNotificationRequest request) {
        List<DeviceToken> tokens = deviceTokenRepository.findByIsActiveTrue();
        return sendToTokens(tokens, request);
    }
    
    /**
     * Send push notification to specific device tokens
     */
    private PushNotificationResponse sendToTokens(List<DeviceToken> tokens, PushNotificationRequest request) {
        int sentCount = 0;
        int failedCount = 0;
        List<String> failedTokens = new ArrayList<>();
        UUID notificationId = UUID.randomUUID();
        
        for (DeviceToken token : tokens) {
            try {
                Message.Builder messageBuilder = Message.builder()
                    .setToken(token.getDeviceToken())
                    .setNotification(Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getBody())
                        .setImage(request.getImageUrl())
                        .build());
                
                // Add custom data
                if (request.getData() != null && !request.getData().isEmpty()) {
                    messageBuilder.putAllData(request.getData());
                }
                
                // Add action URL if provided
                if (request.getActionUrl() != null) {
                    messageBuilder.putData("action_url", request.getActionUrl());
                }
                
                Message message = messageBuilder.build();
                firebaseMessaging.send(message);
                
                // Mark token as used
                token.markAsUsed();
                deviceTokenRepository.save(token);
                
                sentCount++;
                
            } catch (FirebaseMessagingException e) {
                failedCount++;
                failedTokens.add(token.getDeviceToken());
                
                // Deactivate token if it's invalid
                if (e.getMessage().contains("InvalidRegistration") || 
                    e.getMessage().contains("NotRegistered")) {
                    token.deactivate();
                    deviceTokenRepository.save(token);
                }
            }
        }
        
        return PushNotificationResponse.builder()
            .success(failedCount == 0)
            .message(failedCount == 0 ? "All notifications sent successfully" : 
                    String.format("Sent %d, failed %d", sentCount, failedCount))
            .notificationId(notificationId)
            .sentCount(sentCount)
            .failedCount(failedCount)
            .failedTokens(failedTokens)
            .sentAt(LocalDateTime.now())
            .build();
    }
}
