package ai.eventplanner.pushnotification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for push notification operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationResponse {
    
    private boolean success;
    private String message;
    private UUID notificationId;
    private int sentCount;
    private int failedCount;
    private List<String> failedTokens;
    private LocalDateTime sentAt;
}
