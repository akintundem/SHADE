package eventplanner.common.communication.services.channel.email.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result DTO for email sending operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResult {

    private boolean success;
    private String messageId;
    private String errorMessage;
}
