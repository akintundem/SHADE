package eventplanner.common.communication.services.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result DTO for push notification sending operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushResult {
    
    private boolean success;
    private String messageId;
    private String errorMessage;
}

