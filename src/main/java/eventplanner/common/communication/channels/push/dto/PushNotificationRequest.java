package eventplanner.common.communication.channels.push.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for sending push notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationRequest {
    
    private UUID userId;
    private UUID eventId;
    private String title;
    private String body;
    private Map<String, String> data;
    private String imageUrl;
    private String actionUrl;
}
