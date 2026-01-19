package eventplanner.common.communication.services.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal response DTO for bulk notification operations.
 * Used by BulkNotificationService for internal operations only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationResponse {
    
    private boolean success;
    private int totalRecipients;
    private int successCount;
    private int failureCount;
    private String messageId;
    private String errorMessage;
}

