package ai.eventplanner.pushnotification.service;

import ai.eventplanner.pushnotification.dto.PushNotificationRequest;
import ai.eventplanner.pushnotification.dto.PushNotificationResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Integration service for easy push notification sending from anywhere in the application
 */
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class PushNotificationIntegrationService {
    
    private final FirebasePushNotificationService pushNotificationService;
    
    public PushNotificationIntegrationService(FirebasePushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }
    
    /**
     * Send a simple notification to a user
     */
    public PushNotificationResponse sendToUser(UUID userId, String title, String body) {
        PushNotificationRequest request = PushNotificationRequest.builder()
            .userId(userId)
            .title(title)
            .body(body)
            .build();
        return pushNotificationService.sendToUser(request);
    }
    
    /**
     * Send a notification with data to a user
     */
    public PushNotificationResponse sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        PushNotificationRequest request = PushNotificationRequest.builder()
            .userId(userId)
            .title(title)
            .body(body)
            .data(data)
            .build();
        return pushNotificationService.sendToUser(request);
    }
    
    /**
     * Send a notification to all attendees of an event
     */
    public PushNotificationResponse sendToEvent(UUID eventId, String title, String body) {
        PushNotificationRequest request = PushNotificationRequest.builder()
            .eventId(eventId)
            .title(title)
            .body(body)
            .build();
        return pushNotificationService.sendToEvent(request);
    }
    
    /**
     * Send a notification with data to all attendees of an event
     */
    public PushNotificationResponse sendToEvent(UUID eventId, String title, String body, Map<String, String> data) {
        PushNotificationRequest request = PushNotificationRequest.builder()
            .eventId(eventId)
            .title(title)
            .body(body)
            .data(data)
            .build();
        return pushNotificationService.sendToEvent(request);
    }
    
    /**
     * Send a notification to multiple users
     */
    public PushNotificationResponse sendToUsers(List<UUID> userIds, String title, String body) {
        PushNotificationRequest request = PushNotificationRequest.builder()
            .title(title)
            .body(body)
            .build();
        return pushNotificationService.sendToUsers(userIds, request);
    }
    
    /**
     * Send a notification with data to multiple users
     */
    public PushNotificationResponse sendToUsers(List<UUID> userIds, String title, String body, Map<String, String> data) {
        PushNotificationRequest request = PushNotificationRequest.builder()
            .title(title)
            .body(body)
            .data(data)
            .build();
        return pushNotificationService.sendToUsers(userIds, request);
    }
    
    /**
     * Send a notification to all users
     */
    public PushNotificationResponse sendToAll(String title, String body) {
        PushNotificationRequest request = PushNotificationRequest.builder()
            .title(title)
            .body(body)
            .build();
        return pushNotificationService.sendToAll(request);
    }
    
    /**
     * Send a notification with data to all users
     */
    public PushNotificationResponse sendToAll(String title, String body, Map<String, String> data) {
        PushNotificationRequest request = PushNotificationRequest.builder()
            .title(title)
            .body(body)
            .data(data)
            .build();
        return pushNotificationService.sendToAll(request);
    }
}
