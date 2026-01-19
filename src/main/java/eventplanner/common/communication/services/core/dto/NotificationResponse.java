package eventplanner.common.communication.services.core.dto;

import eventplanner.common.communication.enums.CommunicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for notification sending operations
 * Represents both success and failure cases
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    /**
     * Whether the notification was sent successfully
     */
    private boolean success;
    
    /**
     * The ID of the communication record created
     */
    private UUID communicationId;
    
    /**
     * External message ID from the notification service (e.g., email provider, FCM)
     * Only present on success
     */
    private String messageId;
    
    /**
     * Status of the communication record
     */
    private CommunicationStatus status;
    
    /**
     * Error message if the notification failed
     * Only present on failure
     */
    private String errorMessage;
    
    /**
     * Convenience method to create a success response
     */
    public static NotificationResponse success(UUID communicationId, String messageId, CommunicationStatus status) {
        return NotificationResponse.builder()
                .success(true)
                .communicationId(communicationId)
                .messageId(messageId)
                .status(status)
                .build();
    }
    
    /**
     * Convenience method to create a failure response
     */
    public static NotificationResponse failure(UUID communicationId, String errorMessage, CommunicationStatus status) {
        return NotificationResponse.builder()
                .success(false)
                .communicationId(communicationId)
                .errorMessage(errorMessage)
                .status(status)
                .build();
    }
}

